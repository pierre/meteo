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

/**
 * Additive Holt-Winters prediction function
 */
public class HoltWintersComputer
{
    private double value;

    private double alpha;
    private double beta;
    private double gamma;

    private double baseline;
    private double slope;
    private double[] seasonalValues;
    private int seenValues = 0;

    private final FILTERING_TYPE filteringType;
    private double lastForecast;

    enum FILTERING_TYPE
    {
        SIMPLE,
        DOUBLE,
        TRIPLE
    }

    /**
     * Simple exponential smoothing
     *
     * @param alpha alpha parameter (baseline)
     */
    public HoltWintersComputer(double alpha)
    {
        this.alpha = alpha;
        this.filteringType = FILTERING_TYPE.SIMPLE;
    }

    /**
     * Double exponential smoothing
     *
     * @param alpha alpha parameter (baseline)
     * @param beta  beta parameter (slope)
     */
    public HoltWintersComputer(double alpha, double beta)
    {
        this.alpha = alpha;
        this.beta = beta;
        this.filteringType = FILTERING_TYPE.DOUBLE;
    }

    public HoltWintersComputer(double alpha, double beta, double gamma, int period)
    {
        this.alpha = alpha;
        this.beta = beta;
        this.gamma = gamma;
        this.seasonalValues = new double[period];
        this.filteringType = FILTERING_TYPE.TRIPLE;
    }

    public void addNextValue(double value)
    {
        lastForecast = getForecast(1);
        this.value = value;
        updateParameters();
        seenValues++;
    }

    public double getDeviation()
    {
        return Math.abs(value - lastForecast);
    }

    /**
     * Yhat[t+h] = a[t] + h * b[t] + s[t + 1 + (h - 1) mod p]
     * slope is the current best estimate of the trend
     * baseline is the current smoothed value
     *
     * @param h how far to predict
     * @return prediction at +h in the future based on the raw data up to now
     */
    public double getForecast(int h)
    {
        if (filteringType == FILTERING_TYPE.SIMPLE) {
            return baseline;
        }
        else if (filteringType == FILTERING_TYPE.DOUBLE) {
            return baseline + h * slope;
        }
        else if (filteringType == FILTERING_TYPE.TRIPLE) {
            return baseline + h * slope + seasonalValues[(seasonalValues.length - 1 + (h - 1) % seasonalValues.length) % seasonalValues.length];
        }
        else {
            throw new IllegalStateException("Computer not initialized properly");
        }
    }

    /**
     * Yhat[t+h] = a[t] + h * b[t] + s[t + 1 + (h - 1) mod p],
     * <p/>
     * where a[t], b[t] and s[t] are given by
     * <p/>
     * a[t] = alpha (Y[t] - s[t-p]) + (1-alpha) (a[t-1] + b[t-1])
     * b[t] = beta (a[t] - a[t-1]) + (1-beta) b[t-1]
     * s[t] = gamma (Y[t] - a[t]) + (1-gamma) s[t-p]
     */
    private void updateParameters()
    {
        switch (filteringType) {
            case SIMPLE:
                updateParametersSimpleExponential();
                break;
            case DOUBLE:
                updateParametersDoubleExponential();
                break;
            case TRIPLE:
                updateParametersTripleExponential();
                break;
            default:
                throw new IllegalStateException("Computer not initialized properly");
        }
    }

    private void updateParametersTripleExponential()
    {
        if (seenValues == 0) {
            baseline = value;
            seasonalValues[seenValues] = value;
        }
        else if (seenValues == 1) {
            slope = value - baseline;
            baseline = value;
            seasonalValues[seenValues] = value;
        }
        // Initialize seasonal values
        else if (seenValues < seasonalValues.length) {
            seasonalValues[seenValues] = value;
        }
        else {
            double oldBaseline = baseline;
            double oldSlope = slope;
            double oldSeasonal = seasonalValues[0];

            // Move the seasonal window
            System.arraycopy(seasonalValues, 1, seasonalValues, 0, seasonalValues.length - 1);

            baseline = alpha * (value - oldSeasonal) + (1.0 - alpha) * (oldBaseline + oldSlope);
            slope = beta * (baseline - oldBaseline) + (1.0 - beta) * oldSlope;
            seasonalValues[seasonalValues.length - 1] = gamma * (value - baseline) + (1.0 - gamma) * oldSeasonal;
        }
    }

    private void updateParametersDoubleExponential()
    {
        // Initialize baseline using the second seen value (y2)
        // Initialize slope using the first two values (y2 - y1)
        if (seenValues == 0) {
            baseline = value;
        }
        else if (seenValues == 1) {
            slope = value - baseline;
            baseline = value;
        }
        else {
            double oldBaseline = baseline;
            double oldSlope = slope;
            baseline = alpha * value + (1.0 - alpha) * (oldBaseline + oldSlope);
            slope = beta * (baseline - oldBaseline) + (1.0 - beta) * oldSlope;
        }
    }

    private void updateParametersSimpleExponential()
    {
        // Initialize baseline using the first seen value (y1)
        if (seenValues == 0) {
            baseline = value;
        }
        else {
            double oldBaseline = baseline;
            baseline = alpha * value + (1.0 - alpha) * oldBaseline;
        }
    }

    @Override
    public String toString()
    {
        return String.format("alpha = %f, beta = %f, gamma = %f\n", alpha, beta, gamma);
    }

    public double getAlpha()
    {
        return alpha;
    }

    public double getBeta()
    {
        return beta;
    }

    public double getGamma()
    {
        return gamma;
    }

    public double[] getSeasonal()
    {
        return seasonalValues;
    }
}
