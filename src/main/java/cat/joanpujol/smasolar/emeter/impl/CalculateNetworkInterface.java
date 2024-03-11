package cat.joanpujol.smasolar.emeter.impl;

import cat.joanpujol.smasolar.emeter.EMeterConfig;
import io.netty.util.internal.StringUtil;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Optional;

/**
 * Find network interface to use given an {@link EMeterConfig}. If it's not specified it tries to
 * find first IPV4 available interface
 */
public class CalculateNetworkInterface {
  public NetworkInterface calculateNetworkInterface(EMeterConfig config) throws SocketException {
    NetworkInterface networkInterface;
    if (!StringUtil.isNullOrEmpty(config.getNetworkInterface())) {
      networkInterface = NetworkInterface.getByName(config.getNetworkInterface());
    } else {
      Optional<NetworkInterface> firstNetoworkInterface =
          NetworkInterface.networkInterfaces().filter(this::interfaceHasIPV4Address).findFirst();
      if (firstNetoworkInterface.isPresent()) networkInterface = firstNetoworkInterface.get();
      else throw new SocketException("Can't find any network interface");
    }
    return networkInterface;
  }

  private boolean interfaceHasIPV4Address(NetworkInterface networkInterface) {
    Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
    boolean has = false;
    while (addresses.hasMoreElements() && !has) {
      InetAddress address = addresses.nextElement();
      has = address instanceof Inet4Address;
    }
    return has;
  }
}
