package ru.inversion.LoaderMicexFX.model;

public class GatewayConnectionSettings {

    private MicexConnectionType connectionType = MicexConnectionType.TCP2;
    
    private String comPort;
    private String comBaudrate;
    
    private String tcpHost;
    private String tcpService;
    
    private String customConnectText;

    private String packetSize;
    
    private String host;
    private String apiInterface;
    private String server;
    private String service;
    private String broadcast;
    private String prefBroadcast;
    private String userId;
    private String password;
    
    private String feedback;
    
    private String connectionText;

    public String getPacketSize() {
        return packetSize;
    }

    public void setPacketSize(String packetSize) {
        this.packetSize = packetSize;
    }

    public String getApiInterface() {
        return apiInterface;
    }

    public void setApiInterface(String apiInterface) {
        this.apiInterface = apiInterface;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getBroadcast() {
        return broadcast;
    }

    public void setBroadcast(String broadcast) {
        this.broadcast = broadcast;
    }

    public String getPrefBroadcast() {
        return prefBroadcast;
    }

    public void setPrefBroadcast(String prefBroadcast) {
        this.prefBroadcast = prefBroadcast;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public String getConnectionText() {
        return connectionText;
    }

    public void setConnectionText(String connectionText) {
        this.connectionText = connectionText;
    }

    public MicexConnectionType getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(MicexConnectionType connectionType) {
        this.connectionType = connectionType;
    }

    public String getComPort() {
        return comPort;
    }

    public void setComPort(String comPort) {
        this.comPort = comPort;
    }

    public String getComBaudrate() {
        return comBaudrate;
    }

    public void setComBaudrate(String comBaudrate) {
        this.comBaudrate = comBaudrate;
    }

    public String getTcpHost() {
        return tcpHost;
    }

    public void setTcpHost(String tcpHost) {
        this.tcpHost = tcpHost;
    }

    public String getTcpService() {
        return tcpService;
    }

    public void setTcpService(String tcpService) {
        this.tcpService = tcpService;
    }

    public String getCustomConnectText() {
        return customConnectText;
    }

    public void setCustomConnectText(String customConnectText) {
        this.customConnectText = customConnectText;
    }
}
