/*
 * Copyright 2010-2013 Ning, Inc.
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

import com.espertech.esper.collection.SortedDoubleVector;
import com.espertech.esper.epl.agg.AggregationSupport;
import com.espertech.esper.epl.agg.AggregationValidationContext;
import org.apache.log4j.Logger;

public class TPAggregator extends AggregationSupport
{
    private final Logger log = Logger.getLogger(TPAggregator.class);

    private SortedDoubleVector vector;
    private int percentile;

    public void clear()
    {
        vector.clear();
    }

    /**
     * Implemented by plug-in aggregation functions to allow such functions to validate the
     * type of values passed to the function at statement compile time and to generally
     * interrogate parameter expressions.
     *
     * @param validationContext expression information
     */
    @Override
    public void validate(AggregationValidationContext validationContext)
    {
        if (!(validationContext.getParameterTypes()[0].isAssignableFrom(Integer.class))) {
            throw new IllegalArgumentException("TPAggregator takes only numerical arguments");
        }
    }

    public TPAggregator()
    {
        this.vector = new SortedDoubleVector();
    }

    @Override
    public void enter(Object object)
    {
        if (object == null) {
            return;
        }

        // Invoked as follow: tp(90, timeToFirstByte) for tp99
        Object[] params = (Object[]) object;
        percentile = ((Integer) params[0]);

        try {
            double value = Double.parseDouble(params[1].toString());
            vector.add(value);
        }
        catch (ClassCastException ex) {
            // Ignore?
            log.debug(ex);
        }
    }

    @Override
    public void leave(Object object)
    {
        if (object == null) {
            return;
        }

        Object[] params = (Object[]) object;

        try {
            double value = Double.parseDouble(params[1].toString());
            vector.remove(value);
        }
        catch (IllegalStateException ex) {
            // java.lang.IllegalStateException: Value not found in collection
            // We come here if enter() throws an exception
        }
        catch (ClassCastException ex) {
        }
    }

    @Override
    public Object getValue()
    {
        int nbElements = vector.size();

        if (nbElements == 0) {
            return null;
        }

        double rank = percentile / 100. * (nbElements - 1) + 1;
        int k = (int) rank;
        double d = rank - k;

        if (k == 0) {
            return vector.getValue(0);
        }
        else if (k == nbElements) {
            return vector.getValue(nbElements - 1);
        }
        else {
            return vector.getValue(k - 1) + d * (vector.getValue(k) - vector.getValue(k - 1));
        }
    }

    @Override
    public Class getValueType()
    {
        return Double.class;
    }
}
