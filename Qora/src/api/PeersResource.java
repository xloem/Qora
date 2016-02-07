package api;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import controller.Controller;
import database.DBSet;
import database.PeerMap.PeerInfo;
import network.Peer;
import network.PeerManager;
import ntp.NTP;
import utils.DateTimeFormat;

@Path("peers")
@Produces(MediaType.APPLICATION_JSON)
public class PeersResource 
{
	@SuppressWarnings("unchecked")
	@GET
	public String getPeers()
	{
		List<Peer> peers = Controller.getInstance().getActivePeers();
		JSONArray array = new JSONArray();
		
		for(Peer peer: peers)
		{
			array.add(peer.getAddress().getHostAddress());
		}
		
		return array.toJSONString();
	}
	
	@GET
	@Path("info")
	public String getInfo()
	{
		return getTest();
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("height")
	public String getTest()
	{
		Map<Peer,Integer> peers = Controller.getInstance().getPeerHeights();
		JSONArray array = new JSONArray();
		
		for(Map.Entry<Peer, Integer> peer: peers.entrySet())
		{
			JSONObject o = new JSONObject();
			o.put("peer", peer.getKey().getAddress().getHostAddress());
			o.put("height", peer.getValue());
			o.put("ping", peer.getKey().getPing());
			o.put("onlineTime", (NTP.getTime() - peer.getKey().getConnectionTime())/1000);

			array.add(o);
		}
		
		return array.toJSONString();
	}
	
	@SuppressWarnings("unchecked")
	@GET
	@Path("best")
	public String getTopPeers()
	{
		List<Peer> peers = PeerManager.getInstance().getBestPeers();
		JSONArray array = new JSONArray();
		
		for(Peer peer: peers)
		{
			array.add(peer.getAddress().getHostAddress());
		}
		
		return array.toJSONString();
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@GET
	@Path("stat")
	public String getFull() throws UnknownHostException
	{
		List<PeerInfo> iplist = DBSet.getInstance().getPeerMap().getAllPeers(1000);
		
		Map output=new LinkedHashMap();

		for(PeerInfo peer: iplist)
		{
			Map o = new LinkedHashMap();
			
			o.put("findingTime", DateTimeFormat.timestamptoString(peer.getFindingTime()));
			o.put("findingTimeStamp", peer.getFindingTime());

			if(peer.getWhiteConnectTime()>0) {
				o.put("lastWhite", DateTimeFormat.timestamptoString(peer.getWhiteConnectTime()));
				o.put("lastWhiteTimeStamp", peer.getWhiteConnectTime());

			}
			else{
				o.put("lastWhite", "never");
			}
			if(peer.getGrayConnectTime()>0) {
				o.put("lastGray", DateTimeFormat.timestamptoString(peer.getGrayConnectTime()));
				o.put("lastGrayTimeStamp", peer.getGrayConnectTime());
			}
			else{
				o.put("lastGray", "never");
			}
			o.put("whitePingCounter", peer.getWhitePingCouner());
			output.put(InetAddress.getByAddress(peer.getAddress()).getHostAddress(), o);
		}
		
		return JSONValue.toJSONString(output);
	}
	
	@DELETE
	@Path("/known")
	public String clearPeers()
	{
		DBSet.getInstance().getPeerMap().reset();
		
		return "OK";
	}
}
