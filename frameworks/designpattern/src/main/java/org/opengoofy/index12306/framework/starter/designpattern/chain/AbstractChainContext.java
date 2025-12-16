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

package org.opengoofy.index12306.framework.starter.designpattern.chain;

import org.opengoofy.index12306.framework.starter.bases.ApplicationContextHolder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 抽象责任链上下文
 * 公众号：马丁玩编程，回复：加群，添加马哥微信（备注：12306）获取项目资料
 */
public final class AbstractChainContext<T> implements CommandLineRunner {

     private final Map<String,List<AbstractChainHandler>> chainHandlerContains = new HashMap<>();

    /**
     *
     * @param mark  业务对应标识
     * @param requestParam  需要处理的请求参数
     */
    public void handler(String mark,T requestParam){
        //从处理器容器中获取当前业务的处理器集合
        List<AbstractChainHandler> abstractChainHandlers = chainHandlerContains.get(mark);
        //判断集合是否为空，如果为空，则表示该业务标识不存在
        if(CollectionUtils.isEmpty(abstractChainHandlers)){
            throw new RuntimeException(String.format("[%s] Chain of Responsibility ID is undefined.", mark));
        }
        //遍历集合，调用每个处理器的handler方法
        for (AbstractChainHandler abstractChainHandler : abstractChainHandlers) {
            abstractChainHandler.handler(requestParam);
        }
    }

    @Override
    /*
    * 在run方法中对处理器容器进行初始化*/
    public void run(String... args) throws Exception {
    //从IOC容器中获取所有的处理器
        Map<String, AbstractChainHandler> chainBeans = ApplicationContextHolder.getBeansOfType(AbstractChainHandler.class);
    //遍历chainBeans集合
    for(Map.Entry<String, AbstractChainHandler> entry : chainBeans.entrySet()){
        //从容器中获取当前处理器对应业务的处理器集合
        List<AbstractChainHandler> abstractChainHandlers = chainHandlerContains.get(entry.getValue().mark());
        //判断当前业务处理器集合是否为空，为空则创建对象
        if(CollectionUtils.isEmpty(abstractChainHandlers)){
            abstractChainHandlers = new ArrayList<>();
        }
        //将当前处理器放入其业务处理器集合中
        abstractChainHandlers.add(entry.getValue());
        //为当前业务处理器集合中的元素根据优先级进行重新排序
        List<AbstractChainHandler> sortList = abstractChainHandlers.stream().sorted(Comparator.comparing(Ordered::getOrder)).collect(Collectors.toList());
        //将当前业务处理器集合存入处理器容器中，key为当前业务的mark
        chainHandlerContains.put(entry.getValue().mark(),sortList);
        }
    }


//    private final Map<String, List<AbstractChainHandler>> abstractChainHandlerContainer = new HashMap<>();
//
//    /**
//     * 责任链组件执行
//     *
//     * @param mark         责任链组件标识
//     * @param requestParam 请求参数
//     */
//    public void handler(String mark, T requestParam) {
//        List<AbstractChainHandler> abstractChainHandlers = abstractChainHandlerContainer.get(mark);
//        if (CollectionUtils.isEmpty(abstractChainHandlers)) {
//            throw new RuntimeException(String.format("[%s] Chain of Responsibility ID is undefined.", mark));
//        }
//        abstractChainHandlers.forEach(each -> each.handler(requestParam));
//    }
//
//    @Override
//    public void run(String... args) throws Exception {
//        Map<String, AbstractChainHandler> chainFilterMap = ApplicationContextHolder
//                .getBeansOfType(AbstractChainHandler.class);
//        chainFilterMap.forEach((beanName, bean) -> {
//            List<AbstractChainHandler> abstractChainHandlers = abstractChainHandlerContainer.get(bean.mark());
//            if (CollectionUtils.isEmpty(abstractChainHandlers)) {
//                abstractChainHandlers = new ArrayList();
//            }
//            abstractChainHandlers.add(bean);
//            List<AbstractChainHandler> actualAbstractChainHandlers = abstractChainHandlers.stream()
//                    .sorted(Comparator.comparing(Ordered::getOrder))
//                    .collect(Collectors.toList());
//            abstractChainHandlerContainer.put(bean.mark(), actualAbstractChainHandlers);
//        });
//    }
}
