/*
 * Copyright 2010-2011 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.metrics.meteo.esper;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.SingleEventIterator;
import com.espertech.esper.core.StatementContext;
import com.espertech.esper.epl.expression.ExprNode;
import com.espertech.esper.view.ViewSupport;
import com.ning.metrics.meteo.publishers.EsperListener;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HoltWinters extends ViewSupport
{
    private final EventType eventType;
    private final StatementContext context;
    private final ExprNode fieldNameX;
    private final EventBean[] eventsPerStream = new EventBean[1];
    private final HoltWintersComputer computer;
    private EventBean lastNewEvent;
    private double lastRaw;
    private static final double ALPHA = 0.5;
    private HoltWintersComputer deviationComputer;
    private long currentEventTimeStamp;

    public HoltWinters(EventType eventType, StatementContext context, ExprNode fieldNameX, double alpha)
    {
        this.eventType = eventType;
        this.context = context;
        this.fieldNameX = fieldNameX;
        this.computer = new HoltWintersComputer(alpha);
        this.deviationComputer = new HoltWintersComputer(ALPHA);
    }

    public HoltWinters(EventType eventType, StatementContext context, ExprNode fieldNameX, double alpha, double beta)
    {
        this.eventType = eventType;
        this.context = context;
        this.fieldNameX = fieldNameX;
        this.computer = new HoltWintersComputer(alpha, beta);
        this.deviationComputer = new HoltWintersComputer(ALPHA);
    }

    public static EventType createEventType(StatementContext context)
    {
        Map<String, Object> schemaMap = new HashMap<String, Object>();

        schemaMap.put("timestamp", long.class);
        schemaMap.put("forecast", double.class);
        schemaMap.put("lastRaw", double.class);
        schemaMap.put("deviation", double.class);
        schemaMap.put("smoothedDeviation", double.class);
        return context.getEventAdapterService().createAnonymousMapType(schemaMap);
    }

    public HoltWinters(EventType eventType, StatementContext context, ExprNode fieldNameX, double alpha, double beta, double gamma, int period)
    {
        this.eventType = eventType;
        this.context = context;
        this.fieldNameX = fieldNameX;
        this.computer = new HoltWintersComputer(alpha, beta, gamma, period);
    }

    private void processSingleValue(double value)
    {
        computer.addNextValue(value);
        deviationComputer.addNextValue(computer.getDeviation());
    }

    @Override
    public void update(EventBean[] newData, EventBean[] oldData)
    {
        EventBean oldDataEvent = getDataEvent();

        if (this.hasViews()) {
            EventBean newDataEvent = getDataEvent();

            if (lastNewEvent == null) {
                updateChildren(new EventBean[]{newDataEvent}, new EventBean[]{oldDataEvent});
            }
            else {
                updateChildren(new EventBean[]{newDataEvent}, new EventBean[]{lastNewEvent});
            }
            lastNewEvent = newDataEvent;
        }
        if (newData != null) {
            for (EventBean event : newData) {
                currentEventTimeStamp = EsperListener.getEventMillis(event, "timestamp");
                eventsPerStream[0] = event;

                Number number = ((Number) fieldNameX.getExprEvaluator().evaluate(eventsPerStream, true, context));
                if (number != null) {
                    lastRaw = number.doubleValue();
                    processSingleValue(lastRaw);
                }

            }
        }
    }

    @Override
    public EventType getEventType()
    {
        return eventType;
    }

    @Override
    public Iterator<EventBean> iterator()
    {
        return new SingleEventIterator(getDataEvent());
    }

    private EventBean getDataEvent()
    {
        Map<String, Object> dataMap = new HashMap<String, Object>();

        dataMap.put("timestamp", currentEventTimeStamp);
        dataMap.put("forecast", computer.getForecast(1));
        dataMap.put("lastRaw", lastRaw);
        dataMap.put("deviation", computer.getDeviation());
        dataMap.put("smoothedDeviation", deviationComputer.getDeviation());

        return context.getEventAdapterService().adaptorForTypedMap(dataMap, eventType);
    }
}
