/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alipay.sofa.registry.server.meta.remoting.handler;

import com.alipay.sofa.registry.common.model.GenericResponse;
import com.alipay.sofa.registry.common.model.metaserver.inter.communicate.BaseHeartBeatResponse;
import com.alipay.sofa.registry.common.model.metaserver.inter.communicate.DataHeartBeatResponse;
import com.alipay.sofa.registry.common.model.metaserver.inter.communicate.SessionHeartBeatResponse;
import com.alipay.sofa.registry.common.model.metaserver.nodes.DataNode;
import com.alipay.sofa.registry.server.meta.metaserver.CurrentDcMetaServer;
import com.alipay.sofa.registry.server.meta.slot.SlotManager;
import org.springframework.beans.factory.annotation.Autowired;

import com.alipay.sofa.registry.common.model.Node;
import com.alipay.sofa.registry.common.model.metaserver.RenewNodesRequest;
import com.alipay.sofa.registry.log.Logger;
import com.alipay.sofa.registry.log.LoggerFactory;
import com.alipay.sofa.registry.remoting.Channel;

/**
 * Handle session/data node's heartbeat request
 * @author shangyu.wh
 * @version $Id: RenewNodesRequestHandler.java, v 0.1 2018-03-30 19:58 shangyu.wh Exp $
 */
public class HeartbeatRequestHandler extends AbstractServerHandler<RenewNodesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeartbeatRequestHandler.class);

    @Autowired
    private CurrentDcMetaServer      currentDcMetaServer;

    @Autowired
    private SlotManager              slotManager;

    @Override
    public Object reply(Channel channel, RenewNodesRequest renewNodesRequest) {
        Node renewNode = null;
        try {
            renewNode = renewNodesRequest.getNode();
            currentDcMetaServer.renew(renewNode, renewNodesRequest.getDuration());

            BaseHeartBeatResponse response = null;
            switch (renewNode.getNodeType()) {
                case SESSION:
                    response = new SessionHeartBeatResponse(currentDcMetaServer.getEpoch(),
                            currentDcMetaServer.getSlotTable(), currentDcMetaServer.getClusterMembers(),
                            currentDcMetaServer.getSessionServers());
                    break;
                case DATA:
                    response = new DataHeartBeatResponse(currentDcMetaServer.getEpoch(), currentDcMetaServer.getClusterMembers(),
                            currentDcMetaServer.getSessionServers(), slotManager.getDataNodeManagedSlot((DataNode) renewNode, false));
                    break;
                case META:
                    response = new BaseHeartBeatResponse(currentDcMetaServer.getEpoch(),
                            currentDcMetaServer.getSlotTable(), currentDcMetaServer.getClusterMembers());
                    break;
                default:
                    break;
            }

            return new GenericResponse<BaseHeartBeatResponse>().fillSucceed(response);
        } catch (Throwable e) {
            LOGGER.error("Node " + renewNode + "renew error!", e);
            return new GenericResponse<BaseHeartBeatResponse>().fillFailed("Node " + renewNode
                                                                           + "renew error!");
        }
    }

    @Override
    public Class interest() {
        return RenewNodesRequest.class;
    }

    @Override
    public HandlerType getType() {
        return HandlerType.PROCESSER;
    }

}