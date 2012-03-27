/*
 * Copyright 2010-2012 Ning, Inc.
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

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.StatementContext;
import com.espertech.esper.epl.core.ViewResourceCallback;
import com.espertech.esper.epl.expression.ExprNode;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.view.View;
import com.espertech.esper.view.ViewCapability;
import com.espertech.esper.view.ViewFactory;
import com.espertech.esper.view.ViewFactoryContext;
import com.espertech.esper.view.ViewFactorySupport;
import com.espertech.esper.view.ViewParameterException;

import java.util.Arrays;
import java.util.List;

public class HoltWintersViewFactory implements ViewFactory
{
    private ExprNode fieldName;
    private double alpha;
    private double beta = Double.MIN_VALUE;
    private double gamma = Double.MIN_VALUE;
    private int period;
    private EventType eventType;

    @Override
    public void setViewParameters(ViewFactoryContext context, List<ExprNode> viewParameters) throws ViewParameterException
    {
        fieldName = viewParameters.get(0);

        List<Object> evaluated = ViewFactorySupport.validateAndEvaluate("Holt-Winters view", context.getStatementContext(), viewParameters.subList(1, viewParameters.size()));

        alpha = toNumber("alpha", evaluated.get(0)).doubleValue();

        if (evaluated.size() > 1) {
            beta = toNumber("beta", evaluated.get(1)).doubleValue();

            if (evaluated.size() > 2) {
                gamma = toNumber("gamma", evaluated.get(2)).doubleValue();
                period = toNumber("period", evaluated.get(3)).intValue();
            }
        }
    }

    private Number toNumber(String name, Object evaluated) throws ViewParameterException
    {
        if (!(evaluated instanceof Number)) {
            throw new ViewParameterException("Parameter " + name + " is not a number");
        }
        return (Number) evaluated;
    }

    @Override
    public void attach(EventType parentEventType, StatementContext context, ViewFactory optionalParentViewFactory, List<ViewFactory> parentViewFactories) throws ViewParameterException
    {
        ExprNode[] validated = ViewFactorySupport.validate("Holt-Winters view", parentEventType, context, Arrays.asList(fieldName), false);

        if (!JavaClassHelper.isNumeric(validated[0].getExprEvaluator().getType())) {
            throw new ViewParameterException("The field expression for the Holt-Winters view must be of a numeric type");
        }
        fieldName = validated[0];
        eventType = HoltWinters.createEventType(context);
    }

    @Override
    public boolean canProvideCapability(ViewCapability arg0)
    {
        return false;
    }

    @Override
    public boolean canReuse(View view)
    {
        return false;
    }

    @Override
    public EventType getEventType()
    {
        return eventType;
    }

    @Override
    public View makeView(StatementContext context)
    {
        if (beta == Double.MIN_VALUE) {
            return new HoltWinters(eventType, context, fieldName, alpha);
        }
        else if (gamma == Double.MIN_VALUE) {
            return new HoltWinters(eventType, context, fieldName, alpha, beta);
        }
        else {
            return new HoltWinters(eventType, context, fieldName, alpha, beta, gamma, period);
        }
    }

    @Override
    public void setProvideCapability(ViewCapability capability, ViewResourceCallback callback)
    {
        throw new UnsupportedOperationException();
    }
}
