package cat.joanpujol.smasolar.emeter;

import cat.joanpujol.smasolar.SmaSolarDefaultResources;
import io.netty.channel.EventLoopGroup;
import java.net.InetSocketAddress;

public class EMeterConfig {
  private InetSocketAddress address = new InetSocketAddress("239.12.255.254", 9522);
  private String networkInterface;
  private EventLoopGroup eventLoopGroup;

  private EMeterConfig(Builder builder) {
    if (builder.address != null) setAddress(builder.address);
    setNetworkInterface(builder.multicastOutputInterface);
    setEventLoopGroup(
        builder.eventLoopGroup != null
            ? builder.eventLoopGroup
            : SmaSolarDefaultResources.sharedEventLoop());
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public InetSocketAddress getAddress() {
    return address;
  }

  private void setAddress(InetSocketAddress address) {
    this.address = address;
  }

  public String getNetworkInterface() {
    return networkInterface;
  }

  private void setNetworkInterface(String networkInterface) {
    this.networkInterface = networkInterface;
  }

  public EventLoopGroup getEventLoopGroup() {
    return eventLoopGroup;
  }

  private void setEventLoopGroup(EventLoopGroup eventLoopGroup) {
    this.eventLoopGroup = eventLoopGroup;
  }

  public static final class Builder {
    private InetSocketAddress address;
    private String multicastOutputInterface;
    private EventLoopGroup eventLoopGroup;

    private Builder() {}

    /**
     * Multicast address and port to use to listen for Emeter lectures. If none specified default
     * value is 239.12.255.254:9522
     *
     * @param val address to connect
     */
    public Builder address(InetSocketAddress val) {
      address = val;
      return this;
    }

    /**
     * Network interface name to use to join multicast group. If none specified first one that has a
     * IPV4 address is used
     *
     * @param val address to connect
     */
    public Builder multicastOutputInterface(String val) {
      multicastOutputInterface = val;
      return this;
    }

    /**
     * EventLoopGroup to use handle channel handlers
     *
     * @param val
     * @return
     */
    public Builder eventLoopGroup(EventLoopGroup val) {
      eventLoopGroup = val;
      return this;
    }

    /**
     * Build configuration
     *
     * @return
     */
    public EMeterConfig build() {
      return new EMeterConfig(this);
    }
  }
}
