/*
 * Copyright © 2021 M-iTrust (cto@m-itrust.com). Unauthorized copying of this file, via any medium is strictly prohibited. Proprietary and confidential
 */
package io.mitrust.retriever.poolip.master.core;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

import com.google.common.collect.ImmutableSet;

import io.mitrust.retriever.poolip.master.dao.NodesDao;

public class TestNodeResourceImpl {
	final String someForwardIpHeader = "someForwardIpHeader";
	final String someIp = "someIp";

	final Random random = new SecureRandom(new byte[] { 0 });
	final NodesDao dao = mockDao();

	private NodesDao mockDao() {
		NodesDao dao = Mockito.mock(NodesDao.class);
		when(dao.getNodeWithPid(Mockito.anyString())).thenAnswer(invok -> createRegisteredNode(invok.getArgument(0)));
		return dao;
	}

	private IpSlaveNode createRegisteredNode(String pid) {
		IpSlaveNode registeredNode = new IpSlaveNode();
		registeredNode.setId(random.nextInt());
		registeredNode.setPid(pid);
		registeredNode.setLastHeartbeat(new Timestamp(123));
		registeredNode.setRegistration(new Timestamp(123));
		return registeredNode;
	}

	private IpSlaveNode prepareNodeToRegister() {
		IpSlaveNode registeredNode = new IpSlaveNode();
		registeredNode.setCountry("someCountry");
		registeredNode.setResidential(true);
		return registeredNode;
	}

	@Test
	public void testCountrySFR() {
		IpSlaveResourceImpl impl = new IpSlaveResourceImpl(dao, someForwardIpHeader);
		IpSlaveNode nodeApi = new IpSlaveNode();
		String weirdCountry = "SFR";
		Assert.assertFalse(ImmutableSet.copyOf(Locale.getISOCountries()).contains(weirdCountry));

		nodeApi.setCountry(weirdCountry);
		nodeApi.setSshHostIp(someIp);

		impl.createNode(Mockito.mock(HttpServletRequest.class), nodeApi);

		// https://github.com/cormoran-io/mitrust-backend/issues/4417
		verify(dao).insert(Mockito.argThat(n -> n.getCountry().equals(weirdCountry)));
	}

	@Test
	public void test_list_nodes() {
		when(dao.listNodes(false))
				.thenReturn(new ArrayList<>(List.of(createRegisteredNode("r1"), createRegisteredNode("r2"))));
		IpSlaveResourceImpl impl = new IpSlaveResourceImpl(dao, someForwardIpHeader);

		Assertions.assertThat(impl.list()).hasSize(2);
	}

	@Test
	public void test_random_node() {
		when(dao.listNodes(false))
				.thenReturn(new ArrayList<>(List.of(createRegisteredNode("r1"), createRegisteredNode("r2"))));
		IpSlaveResourceImpl impl = new IpSlaveResourceImpl(dao, someForwardIpHeader);

		Assertions.assertThat(impl.getRandomNode().getPid()).isBetween("r1", "r2");
	}

	@Test(expected = ResponseStatusException.class)
	public void should_handle_empty_nodes() {
		when(dao.listNodes(false)).thenReturn(List.of());
		IpSlaveResourceImpl impl = new IpSlaveResourceImpl(dao, someForwardIpHeader);
		impl.getRandomNode();
	}

	@Test
	public void should_get_existing_node() {
		when(dao.getNodeWithPid(matches("r1"))).thenReturn(createRegisteredNode("r1"));
		IpSlaveResourceImpl impl = new IpSlaveResourceImpl(dao, someForwardIpHeader);
		IpSlaveNode node = impl.getNode("r1");
		Assertions.assertThat(node.getPid()).isEqualTo("r1");
		Assertions.assertThat(node.getLastHeartbeat()).isEqualTo(new Timestamp(123));
		Assertions.assertThat(node.getRegistration()).isEqualTo(new Timestamp(123));
		Assertions.assertThat(node.isMaintenance()).isFalse();
	}

	@Test(expected = ResponseStatusException.class)
	public void should_handle_innexisting_node() {
		when(dao.getNodeWithPid(Mockito.anyString())).thenReturn(null);
		IpSlaveResourceImpl impl = new IpSlaveResourceImpl(dao, someForwardIpHeader);
		impl.getNode("some_pid");
	}

	@Test
	public void should_update_node() {
		IpSlaveResourceImpl impl = new IpSlaveResourceImpl(dao, someForwardIpHeader);

		HttpServletRequest httpRequest = Mockito.mock(HttpServletRequest.class);
		Mockito.when(httpRequest.getRemoteAddr()).thenReturn(someIp);

		impl.heartBeat(httpRequest, "pid1");

		verify(dao).updateHeartbeat("pid1", someIp);
	}

	@Test
	public void should_delete_node() {
		IpSlaveResourceImpl impl = new IpSlaveResourceImpl(dao, someForwardIpHeader);

		impl.deleteNode("pid1");

		verify(dao).delete("pid1");
	}

	@Test
	public void should_create_node() {
		IpSlaveResourceImpl impl = new IpSlaveResourceImpl(dao, someForwardIpHeader);
		IpSlaveNode node = prepareNodeToRegister();

		HttpServletRequest mock = Mockito.mock(HttpServletRequest.class);
		when(mock.getHeader(eq(someForwardIpHeader))).thenReturn("192.168.1.25");

		impl.createNode(mock, node);
		verify(dao).insert(Mockito.argThat(n -> n.getOutboundIp().equals("192.168.1.25")));
	}

}
