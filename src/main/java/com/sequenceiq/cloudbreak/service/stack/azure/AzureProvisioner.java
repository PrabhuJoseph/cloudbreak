package com.sequenceiq.cloudbreak.service.stack.azure;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.AzureLocation;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.AzureVmType;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Port;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.service.credential.azure.AzureCertificateService;
import com.sequenceiq.cloudbreak.service.stack.Provisioner;
import com.sequenceiq.cloudbreak.service.stack.StackCreationFailure;

import groovyx.net.http.HttpResponseDecorator;
import groovyx.net.http.HttpResponseException;
import reactor.core.Reactor;
import reactor.event.Event;

@Component
public class AzureProvisioner implements Provisioner {

    public static final String CREDENTIAL = "credential";
    public static final String EMAILASFOLDER = "emailAsFolder";

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureProvisioner.class);
    private static final int NOT_FOUND = 404;
    private static final String LOCATION = "location";
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String AFFINITYGROUP = "affinityGroup";
    private static final String ADDRESSPREFIX = "addressPrefix";
    private static final String SUBNETADDRESSPREFIX = "subnetAddressPrefix";
    private static final String DEPLOYMENTSLOT = "deploymentSlot";
    private static final String LABEL = "label";
    private static final String IMAGENAME = "imageName";
    private static final String IMAGESTOREURI = "imageStoreUri";
    private static final String HOSTNAME = "hostname";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String SUBNETNAME = "subnetName";
    private static final String CUSTOMDATA = "customData";
    private static final String VIRTUALNETWORKNAME = "virtualNetworkName";
    private static final String VMTYPE = "vmType";
    private static final String SSHPUBLICKEYFINGERPRINT = "sshPublicKeyFingerprint";
    private static final String SSHPUBLICKEYPATH = "sshPublicKeyPath";
    private static final String SERVICENAME = "serviceName";
    private static final String PORTS = "ports";
    private static final String DATA = "data";
    private static final String DEFAULT_USER_NAME = "ubuntu";
    private static final String PRODUCTION = "production";

    @Autowired
    private Reactor reactor;

    @Autowired
    private RetryingStackUpdater retryingStackUpdater;

    @Override
    public void buildStack(Stack stack, String userData, Map<String, Object> setupProperties) {
        AzureTemplate azureTemplate = (AzureTemplate) stack.getTemplate();
        retryingStackUpdater.updateStackStatus(stack.getId(), Status.REQUESTED);
        Credential credential = (Credential) setupProperties.get(CREDENTIAL);
        String emailAsFolder = (String) setupProperties.get(EMAILASFOLDER);

        String filePath = AzureCertificateService.getUserJksFileName(credential, emailAsFolder);
        File file = new File(filePath);
        AzureClient azureClient = new AzureClient(
                ((AzureCredential) credential).getSubscriptionId(), file.getAbsolutePath(), ((AzureCredential) credential).getJks()
        );
        retryingStackUpdater.updateStackStatus(stack.getId(), Status.CREATE_IN_PROGRESS);
        String name = stack.getName().replaceAll("\\s+", "");
        String commonName = ((AzureCredential) credential).getName().replaceAll("\\s+", "");
        createAffinityGroup(azureClient, azureTemplate, commonName);
        createStorageAccount(azureClient, azureTemplate, commonName);
        createVirtualNetwork(azureClient, name, commonName);

        for (int i = 0; i < stack.getNodeCount(); i++) {
            try {
                String vmName = getVmName(name, i);
                createCloudService(azureClient, azureTemplate, name, vmName, commonName);
                createServiceCertificate(azureClient, azureTemplate, vmName, emailAsFolder);
                createVirtualMachine(azureClient, azureTemplate, name, vmName, commonName, emailAsFolder, userData);
            } catch (FileNotFoundException e) {
                LOGGER.info("Problem with the ssh file because not found: " + e.getMessage());
                reactor.notify(ReactorConfig.STACK_CREATE_FAILED_EVENT, Event.wrap(new StackCreationFailure(stack.getId(),
                        "Error while creating Azure stack: ssh file not found")));
                return;
            } catch (CertificateException e) {
                LOGGER.info("Problem with the certificate file: " + e.getMessage());
                reactor.notify(ReactorConfig.STACK_CREATE_FAILED_EVENT, Event.wrap(new StackCreationFailure(stack.getId(),
                        "Error while creating Azure stack: certificate not correct")));
                return;
            } catch (NoSuchAlgorithmException e) {
                LOGGER.info("Problem with the fingerprint: " + e.getMessage());
                reactor.notify(ReactorConfig.STACK_CREATE_FAILED_EVENT, Event.wrap(new StackCreationFailure(stack.getId(),
                        "Error while creating Azure stack: no such algorithm exception")));
                return;
            } catch (Exception ex) {
                LOGGER.info("Problem with the stack creation: " + ex.getMessage());
                reactor.notify(ReactorConfig.STACK_CREATE_FAILED_EVENT, Event.wrap(new StackCreationFailure(stack.getId(),
                        "Error while creating Azure stack: " + ex.getMessage())));
                return;
            }
        }
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    private String getVmName(String azureTemplate, int i) {
        return String.format("%s-%s", azureTemplate, i);
    }

    private void createVirtualMachine(AzureClient azureClient, AzureTemplate azureTemplate,
            String name, String vmName, String commonName, String emailAsFolder, String userData)
            throws FileNotFoundException, CertificateException, NoSuchAlgorithmException {
        byte[] encoded = Base64.encodeBase64(vmName.getBytes());
        String label = new String(encoded);
        Map<String, Object> props = new HashMap<>();
        List<Port> ports = new ArrayList<>();
        ports.add(new Port("Ambari", "8080", "8080", "tcp"));
        ports.add(new Port("NameNode", "50070", "50070", "tcp"));
        ports.add(new Port("ResourceManager", "8088", "8088", "tcp"));
        ports.add(new Port("Job History Server", "19888", "19888", "tcp"));
        ports.add(new Port("HBase Master", "60010", "60010", "tcp"));
        ports.add(new Port("Falcon", "15000", "15000", "tcp"));
        ports.add(new Port("Storm", "8744", "8744", "tcp"));
        ports.add(new Port("Oozie", "11000", "11000", "tcp"));
        ports.add(new Port("HTTP", "80", "80", "tcp"));
        props.put(NAME, vmName);
        props.put(DEPLOYMENTSLOT, PRODUCTION);
        props.put(LABEL, label);
        props.put(IMAGENAME, azureTemplate.getImageName());
        props.put(IMAGESTOREURI,
                String.format("http://%s.blob.core.windows.net/vhd-store/%s.vhd", commonName, vmName)
        );
        props.put(HOSTNAME, vmName);
        props.put(USERNAME, DEFAULT_USER_NAME);
        if (azureTemplate.getPassword() != null && !azureTemplate.getPassword().isEmpty()) {
            props.put(PASSWORD, azureTemplate.getPassword());
        } else {
            X509Certificate sshCert = new X509Certificate(AzureCertificateService.getCerFile(emailAsFolder, azureTemplate.getId()));
            props.put(SSHPUBLICKEYFINGERPRINT, sshCert.getSha1Fingerprint().toUpperCase());
            props.put(SSHPUBLICKEYPATH, String.format("/home/%s/.ssh/authorized_keys", DEFAULT_USER_NAME));
        }
        props.put(SERVICENAME, vmName);
        props.put(SUBNETNAME, name);
        props.put(CUSTOMDATA, new String(Base64.encodeBase64(userData.getBytes())));
        props.put(VIRTUALNETWORKNAME, name);
        props.put(PORTS, ports);
        props.put(VMTYPE, AzureVmType.valueOf(azureTemplate.getVmType()).vmType().replaceAll(" ", ""));
        HttpResponseDecorator virtualMachineResponse = (HttpResponseDecorator) azureClient.createVirtualMachine(props);
        String requestId = (String) azureClient.getRequestId(virtualMachineResponse);
        azureClient.waitUntilComplete(requestId);
    }

    private void createCloudService(AzureClient azureClient, AzureTemplate azureTemplate, String name, String vmName, String commonName) {
        Map<String, String> props = new HashMap<>();
        props.put(NAME, vmName);
        props.put(DESCRIPTION, azureTemplate.getDescription());
        props.put(AFFINITYGROUP, commonName);
        HttpResponseDecorator cloudServiceResponse = (HttpResponseDecorator) azureClient.createCloudService(props);
        String requestId = (String) azureClient.getRequestId(cloudServiceResponse);
        azureClient.waitUntilComplete(requestId);
    }

    private void createServiceCertificate(AzureClient azureClient, AzureTemplate azureTemplate, String name, String emailAsFolder)
            throws FileNotFoundException, CertificateException {
        Map<String, String> props = new HashMap<>();
        props.put(NAME, name);
        X509Certificate sshCert = new X509Certificate(AzureCertificateService.getCerFile(emailAsFolder, azureTemplate.getId()));
        props.put(DATA, new String(sshCert.getPem()));
        HttpResponseDecorator serviceCertificate = (HttpResponseDecorator) azureClient.createServiceCertificate(props);
        String requestId = (String) azureClient.getRequestId(serviceCertificate);
        azureClient.waitUntilComplete(requestId);
    }

    private void createVirtualNetwork(AzureClient azureClient, String name, String commonName) {
        if (!azureClient.getVirtualNetworkConfiguration().toString().contains(name)) {
            Map<String, String> props = new HashMap<>();
            props.put(NAME, name);
            props.put(AFFINITYGROUP, commonName);
            props.put(SUBNETNAME, name);
            props.put(ADDRESSPREFIX, "172.16.0.0/16");
            props.put(SUBNETADDRESSPREFIX, "172.16.0.0/24");
            HttpResponseDecorator virtualNetworkResponse = (HttpResponseDecorator) azureClient.createVirtualNetwork(props);
            String requestId = (String) azureClient.getRequestId(virtualNetworkResponse);
            azureClient.waitUntilComplete(requestId);
        }
    }

    private void createStorageAccount(AzureClient azureClient, AzureTemplate azureTemplate, String commonName) {
        try {
            azureClient.getStorageAccount(commonName);
        } catch (Exception ex) {
            if (((HttpResponseException) ex).getStatusCode() == NOT_FOUND) {
                Map<String, String> props = new HashMap<>();
                props.put(NAME, commonName);
                props.put(DESCRIPTION, azureTemplate.getDescription());
                props.put(AFFINITYGROUP, commonName);
                HttpResponseDecorator storageResponse = (HttpResponseDecorator) azureClient.createStorageAccount(props);
                String requestId = (String) azureClient.getRequestId(storageResponse);
                azureClient.waitUntilComplete(requestId);
            } else {
                throw new InternalServerException(ex.getMessage());
            }
        }
    }

    private void createAffinityGroup(AzureClient azureClient, AzureTemplate azureTemplate, String name) {
        try {
            azureClient.getAffinityGroup(name);
        } catch (Exception ex) {
            if (((HttpResponseException) ex).getStatusCode() == NOT_FOUND) {
                Map<String, String> props = new HashMap<>();
                props.put(NAME, name);
                props.put(LOCATION, AzureLocation.valueOf(azureTemplate.getLocation()).location());
                props.put(DESCRIPTION, azureTemplate.getDescription());
                HttpResponseDecorator affinityResponse = (HttpResponseDecorator) azureClient.createAffinityGroup(props);
                String requestId = (String) azureClient.getRequestId(affinityResponse);
                azureClient.waitUntilComplete(requestId);
            } else {
                throw new InternalServerException(ex.getMessage());
            }
        }
    }
}
