package com.cldcvr.quann.netflow.v5;

import static org.junit.Assert.*;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.cldcvr.quann.netflow.v5.NetFlowV5Header;
import com.cldcvr.quann.netflow.v5.NetFlowV5Packet;
import com.cldcvr.quann.netflow.v5.NetFlowV5Parser;

public class NetFlowV5ParserTest {
	@Test
	public void testParse() {
		
		String hexPayload = "0009001327e2b8985a0b1c71004367580000000101040047000000000000005400000001110000d536ac128044000000040035080808080000000327e2b89827e2b898000000000000000002307301cb7f74c208080808d0e800350104004700000000000000000000000011000000350808080800000003d0e8cb7f74c20000000427e2b89827e2b89800000000000000000230730108080808ac1280440035d53601040047000000000000006000000001110000e826ac128044000000040035080808080000000327e2b89827e2b898000000000000000000c42701cb7f74c208080808984e00350104004700000000000000000000000011000000350808080800000003984ecb7f74c20000000427e2b89827e2b898000000000000000000c4270108080808ac1280440035e8260100003b000000000000003e000000010600020448ac12b72a000000041576c0a8010c0000000027e2b89827e2b89800000000000000000000000301040047000000000000004200000001060002dbcaac124b590000000401bb4a7dc89a0000000327e2b89827e2b8980000000000000000004cb201cb7f74c24a7dc89acc8301bb01040047000000000000004200000001060002dbccac124b590000000401bb4a7dc8660000000327e2b89827e2b898000000000000000003090701cb7f74c24a7dc86643ec01bb01040047000000000000004200000001060002dbceac124b590000000401bb4a7dc89c0000000327e2b89827e2b898000000000000000001664201cb7f74c24a7dc89c086d01bb01040047000000000000004200000001060002dbcbac124b590000000401bb4a7dc88a0000000327e2b89827e2b8980000000000000000006e2b01cb7f74c24a7dc88a836401bb01040047000000000000004200000001060002dbcdac124b590000000401bb4a7dc89d0000000327e2b89827e2b89800000000000000000004ca01cb7f74c24a7dc89dd1b701bb01040047000000000000004200000001060002dbcfac124b590000000401bb4a7dc85f0000000327e2b89827e2b8980000000000000000023f5601cb7f74c24a7dc85f858f01bb01040047000000000000004200000001060002dbd1ac124b590000000401bb4a7dc8880000000327e2b89827e2b898000000000000000003a36601cb7f74c24a7dc888ae1001bb01040047000000000000004200000001060002dbd0ac124b590000000401bb4a7dc8880000000327e2b89827e2b898000000000000000001242401cb7f74c24a7dc888df9301bb01040047000000000000004200000001060002dbd2ac124b590000000401bb4a7dc8880000000327e2b89827e2b898000000000000000003707901cb7f74c24a7dc888e97901bb0104004700000000000000420000000106001201bb4a7dc88800000003df93cb7f74c20000000427e2b89827e2b8980000000000000000012424014a7dc888ac124b5901bbdbd00104004700000000000000420000000106001201bb4a7dc88800000003ae10cb7f74c20000000427e2b89827e2b898000000000000000003a366014a7dc888ac124b5901bbdbd10104004700000000000000420000000106001201bb4a7dc88800000003e979cb7f74c20000000427e2b89827e2b8980000000000000000037079014a7dc888ac124b5901bbdbd2010400470000000000000f3c0000000506000001bb4a7dc89c00000003086dcb7f74c20000000427e2b89827e2b8980000000000000000016642014a7dc89cac124b5901bbdbce0104004700000000000010d00000000506000001bb4a7dc88a000000038364cb7f74c20000000427e2b89827e2b8980000000000000000006e2b014a7dc88aac124b5901bbdbcb";
		byte[] b = toBinary(hexPayload);
		
		NetFlowV5Packet packet = NetFlowV5Parser.parsePacket(b);
		
		System.out.println("Header"+packet.getHeader());
		System.out.println("Records"+packet.getRecords());
		
		try {
			writeToCSV(packet.getRecords(),packet.getHeader());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		assertNotNull(packet);

		NetFlowV5Header h = packet.getHeader();
		assertEquals(9, h.getVersion());
		assertEquals(19, h.getCount());
		assertEquals(669169816, h.getSysUptime());
		assertEquals(1510677617, h.getUnixSecs());
		assertEquals(4417368, h.getUnixNsecs());

		assertEquals(19, packet.getRecords().size());
		Map<String, Object> r = packet.getRecords().get(0).toMap();
		assertEquals("0.0.0.84", r.get("dst_addr"));
		assertEquals(152, r.get("protocol"));
		assertEquals(58040, r.get("src_as"));
		assertEquals("0.0.0.0", r.get("src_addr"));
		assertEquals(807, r.get("dst_port"));
		assertEquals(0, r.get("src_port"));
		assertEquals(0, r.get("src_mask"));
		assertEquals(39, r.get("tos"));
		assertEquals(4352, r.get("input"));
		assertEquals("0.0.0.1", r.get("next_hop"));
		assertEquals(38912, r.get("dst_as"));
		assertEquals(213, r.get("output"));
		assertEquals(0, r.get("dst_mask"));
	}

	private byte[] toBinary(String hex) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		for (int i = 0; i < hex.length(); i += 2) {
			String s = hex.substring(i, i + 2);
			bos.write(Integer.parseInt(s, 16));
		}
		
		return bos.toByteArray();
	}
	private static final String CSV_SEPARATOR = ",";
	private static final String SEPERATOR = "\n";
	private static final String RECORDHEADER = "src_addr,dst_addr,next_hop,input,output,pkts,octets,first,"
			+ "last,src_port,dst_port,tcpflags,protocol,tos,src_as,dst_as,src_mask,dst_mask";
	private static final String HEADER = "ver,count,sysuptime,unixsecs,unixnsecs,seq,engine_type,"
			+ "engine_id,sampling_mode,sampling_interval";
	
	private static void writeToCSV(List<NetFlowV5Record> list, NetFlowV5Header netFlowV5Header ) throws FileNotFoundException {
		
		StringBuffer oneLine = new StringBuffer();
		oneLine.append(HEADER);
		oneLine.append(SEPERATOR);
		oneLine.append(netFlowV5Header.getVersion());
		oneLine.append(CSV_SEPARATOR);
		oneLine.append(netFlowV5Header.getCount());
		oneLine.append(CSV_SEPARATOR);
		oneLine.append(netFlowV5Header.getSysUptime());
		oneLine.append(CSV_SEPARATOR);
		oneLine.append(netFlowV5Header.getUnixSecs());
		oneLine.append(CSV_SEPARATOR);
		oneLine.append(netFlowV5Header.getUnixNsecs());
		oneLine.append(CSV_SEPARATOR);
		oneLine.append(netFlowV5Header.getFlowSequence());
		oneLine.append(CSV_SEPARATOR);
		oneLine.append(netFlowV5Header.getEngineType());
		oneLine.append(CSV_SEPARATOR);
		oneLine.append(netFlowV5Header.getEngineId());
		oneLine.append(CSV_SEPARATOR);
		oneLine.append(netFlowV5Header.getSamplingMode());
		oneLine.append(CSV_SEPARATOR);
		oneLine.append(netFlowV5Header.getSamplingInterval());
		oneLine.append(SEPERATOR);
		oneLine.append(RECORDHEADER);
		oneLine.append(SEPERATOR);
		for (NetFlowV5Record parse : list)
		    {	
			oneLine.append(list.get(list.indexOf(parse)).getSrcAddr());
			oneLine.append(CSV_SEPARATOR);
			oneLine.append(list.get(list.indexOf(parse)).getDstAddr());
			oneLine.append(CSV_SEPARATOR);
			oneLine.append(list.get(list.indexOf(parse)).getNextHop());
			oneLine.append(CSV_SEPARATOR);
			oneLine.append(list.get(list.indexOf(parse)).getInputIface());
			oneLine.append(CSV_SEPARATOR);
			oneLine.append(list.get(list.indexOf(parse)).getOutputIface());
			oneLine.append(CSV_SEPARATOR);
			oneLine.append(list.get(list.indexOf(parse)).getPacketCount());
			oneLine.append(CSV_SEPARATOR);
			oneLine.append(list.get(list.indexOf(parse)).getOctetCount());
			oneLine.append(CSV_SEPARATOR);
			oneLine.append(list.get(list.indexOf(parse)).getFirst());
			oneLine.append(CSV_SEPARATOR);
			oneLine.append(list.get(list.indexOf(parse)).getLast());
			oneLine.append(CSV_SEPARATOR);
			oneLine.append(list.get(list.indexOf(parse)).getSrcPort());
			oneLine.append(CSV_SEPARATOR);
			oneLine.append(list.get(list.indexOf(parse)).getDstPort());
			oneLine.append(CSV_SEPARATOR);
			oneLine.append(list.get(list.indexOf(parse)).getTcpFlags());
			oneLine.append(CSV_SEPARATOR);
			oneLine.append(list.get(list.indexOf(parse)).getProtocol());
			oneLine.append(CSV_SEPARATOR);
			oneLine.append(list.get(list.indexOf(parse)).getTos());
			oneLine.append(CSV_SEPARATOR);
			oneLine.append(list.get(list.indexOf(parse)).getSrcAs());
			oneLine.append(CSV_SEPARATOR);
			oneLine.append(list.get(list.indexOf(parse)).getDstAs());
			oneLine.append(CSV_SEPARATOR);
			oneLine.append(list.get(list.indexOf(parse)).getSrcMask());
			oneLine.append(CSV_SEPARATOR);
			oneLine.append(list.get(list.indexOf(parse)).getDstMask());
			oneLine.append(CSV_SEPARATOR);
			oneLine.append(SEPERATOR);
		    }
		
	}
}
