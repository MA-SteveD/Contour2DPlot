package org.contour2dplot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.IntStream;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Piotr Dzwiniel on 2016-03-21.
 */

/*
 * Copyright 2016 Piotr Dzwiniel
 *
 * This file is part of org.contour2dplot package.
 *
 * org.contour2dplot package is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 *
 * org.contour2dplot package is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with org.contour2dplot package; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

public class Contour2DMap extends Pane {

	private static final Logger LOGGER = LoggerFactory.getLogger(Contour2DMap.class);

	private double sizeX;
	private double sizeY;

	private double[][] data;
	private double isoFactor;
	private int interpolationFactor;
	private String mapColorScale;

	public Contour2DMap() {
		super();
	}

	public Contour2DMap(double sizeX, double sizeY) {
		this();
		this.sizeX = sizeX;
		this.sizeY = sizeY;
	}

	public void draw() {

		double[][] interpolatedData = interpolateData();

		ArrayList<Double> isoValues = getIsoValues(interpolatedData);

		ArrayList<Color> colorScale = getColorScale(isoValues);

		ArrayList<ArrayList<IsoCell>> isoCells = getIsoCells(interpolatedData);

		renderIsoCells(interpolatedData, isoValues, colorScale, isoCells);
	}

	private double[][] interpolateData() {
		ArrayList<ArrayList<Double>> temporalData = new ArrayList<>();
		BicubicInterpolator bicubicInterpolator = new BicubicInterpolator();

		double minDataValue = findMin(data);
		double maxDataValue = findMax(data);

		for (int i = 0; i < data.length * interpolationFactor; i++) {
			double idx = i * (1.0 / interpolationFactor);
			ArrayList<Double> row = new ArrayList<>();
			for (int j = 0; j < data[0].length * interpolationFactor; j++) {
				double jdy = j * (1.0 / interpolationFactor);
				double value = bicubicInterpolator.getValue(data, idx, jdy);

				if (value < minDataValue) {
					value = minDataValue;
				} else if (value > maxDataValue) {
					value = maxDataValue;
				}

				row.add(value);
			}
			temporalData.add(row);
		}

		LOGGER.debug("Temporal data ({} x {}):\n{}", temporalData.size(), temporalData.get(0).size(), temporalData);

		int correction = interpolationFactor - 1;
		double[][] interpolatedData = new double[temporalData.size() - correction][temporalData.get(0).size() - correction];

		for (int i = 0; i < interpolatedData.length; i++) {
			for (int j = 0; j < interpolatedData[i].length; j++) {
				interpolatedData[i][j] = temporalData.get(i).get(j);
			}
		}

		LOGGER.debug("Interpolated data: double[{}][{}]", interpolatedData.length, interpolatedData[0].length);

		return interpolatedData;
	}

	/**
	 * Based on: https://en.wikipedia.org/wiki/Marching_squares#Isoband. For each isoColor draw polygon in the specific isoCell if
	 * ternary index is different than 0 and 80.
	 */
	private void renderIsoCells(double[][] interpolatedData, ArrayList<Double> isoValues, ArrayList<Color> colorScale,
			ArrayList<ArrayList<IsoCell>> isoCells) {

		LOGGER.debug("Rendering ISO cells...");

		for (int i = 0; i < colorScale.size(); i++) {

			double startOfRange = isoValues.get(i);
			double endOfRange = isoValues.get(i + 1);

			for (int j = 0; j < isoCells.size(); j++) {

				ArrayList<IsoCell> isoCellsRow = isoCells.get(j);

				for (int k = 0; k < isoCellsRow.size(); k++) {

					ArrayList<Integer> ternaryNumber = new ArrayList<>();

					// Bottom left corner of the iso cell.
					double average = 0;

					double decibelThreshold = interpolatedData[j + 1][k];
					int ternarySingleValue = checkIfValueIsInRange(startOfRange, endOfRange, decibelThreshold);
					ternaryNumber.add(ternarySingleValue);
					average += decibelThreshold;

					// Bottom right corner of the iso cell.
					decibelThreshold = interpolatedData[j + 1][k + 1];
					ternarySingleValue = checkIfValueIsInRange(startOfRange, endOfRange, decibelThreshold);
					ternaryNumber.add(ternarySingleValue);
					average += decibelThreshold;

					// Top right corner of the iso cell.
					decibelThreshold = interpolatedData[j][k + 1];
					ternarySingleValue = checkIfValueIsInRange(startOfRange, endOfRange, decibelThreshold);
					ternaryNumber.add(ternarySingleValue);
					average += decibelThreshold;

					// Top left corner of the iso cell.
					decibelThreshold = interpolatedData[j][k];
					ternarySingleValue = checkIfValueIsInRange(startOfRange, endOfRange, decibelThreshold);
					ternaryNumber.add(ternarySingleValue);
					average += decibelThreshold;

					int ternaryIndex = ternaryToDecimalConverter(ternaryNumber);
					if (ternaryIndex != 0 && ternaryIndex != 80) {

						int[] saddleIndices = { 10, 11, 19, 20, 23, 30, 33, 47, 50, 57, 60, 61, 69, 70 };
						boolean contains = IntStream.of(saddleIndices).anyMatch(x -> x == ternaryIndex);
						if (contains) {
							average /= 4;
							int ternaryIndexOfAverageOfCorners = checkIfValueIsInRange(startOfRange, endOfRange, average);
							isoCellsRow.get(k).setTernaryIndexOfAverageOfCorners(ternaryIndexOfAverageOfCorners);
							isoCellsRow.get(k).drawIsoBand(ternaryIndex, colorScale.get(i));
						} else {
							isoCellsRow.get(k).drawIsoBand(ternaryIndex, colorScale.get(i));
						}
					}
				}
			}
		}
	}

	private ArrayList<Double> getIsoValues(double[][] interpolatedData) {
		double minDataValue;
		double maxDataValue;
		minDataValue = findMin(interpolatedData);
		maxDataValue = findMax(interpolatedData);

		double[] isoValues = arange(minDataValue, maxDataValue, isoFactor);
		ArrayList<Double> arrayListOfIsoValues = new ArrayList<>();

		for (double isoValue : isoValues) {
			arrayListOfIsoValues.add(isoValue);
		}
		arrayListOfIsoValues.add(maxDataValue);
		return arrayListOfIsoValues;
	}

	private ArrayList<Color> getColorScale(ArrayList<Double> arrayListOfIsoValues) {
		int numberOfColorIntervals = arrayListOfIsoValues.size() - 1;
		ArrayList<Color> colorScale = new ArrayList<>();

		switch (mapColorScale) {
			case "Monochromatic":

				ArrayList<Double> brightnessValues = linspace(0, 100, numberOfColorIntervals, true);

				for (int i = 0; i < brightnessValues.size(); i++) {
					colorScale.add(Color.hsb(0, 0, brightnessValues.get(i) / 100));
				}

				break;
			case "Color":

				ArrayList<Double> hueValues = linspace(0, 250, numberOfColorIntervals, true);
				Collections.reverse(hueValues);

				for (int i = 0; i < hueValues.size(); i++) {
					colorScale.add(Color.hsb(hueValues.get(i), 1.0, 1.0));
				}

				break;
		}
		return colorScale;
	}

	private ArrayList<ArrayList<IsoCell>> getIsoCells(double[][] interpolatedData) {
		int isoCellsNumberX = interpolatedData[0].length - 1;
		int isoCellsNumberY = interpolatedData.length - 1;

		double isoCellSizeX = sizeX / isoCellsNumberX;
		double isoCellSizeY = sizeY / isoCellsNumberY;

		double isoCellPositionX = 0;
		double isoCellPositionY = 0;

		ArrayList<ArrayList<IsoCell>> matrixOfIsoCells = new ArrayList<>();

		for (int i = 0; i < isoCellsNumberY; i++) {

			ArrayList<IsoCell> oneRowOfIsoCells = new ArrayList<>();

			for (int j = 0; j < isoCellsNumberX; j++) {

				IsoCell isoCell = new IsoCell(isoCellSizeX, isoCellSizeY);

				isoCell.setLayoutX(isoCellPositionX);
				isoCell.setLayoutY(isoCellPositionY);

				this.getChildren().add(isoCell);

				oneRowOfIsoCells.add(isoCell);

				isoCellPositionX += isoCellSizeX;
			}
			matrixOfIsoCells.add(oneRowOfIsoCells);

			isoCellPositionX = 0;
			isoCellPositionY += isoCellSizeY;
		}
		return matrixOfIsoCells;
	}

	private double findMin(double[][] matrix) {
		double min = matrix[0][0];
		for (double[] element : matrix) {
			for (int column = 0; column < element.length; column++) {
				if (min > element[column]) {
					min = element[column];
				}
			}
		}
		return min;
	}

	private double findMax(double[][] matrix) {
		double max = matrix[0][0];
		for (double[] element : matrix) {
			for (int column = 0; column < element.length; column++) {
				if (max < element[column]) {
					max = element[column];
				}
			}
		}
		return max;
	}

	public double[] arange(double start, double end, double step) {
		return IntStream.rangeClosed(0, (int) ((end - start) / step)).mapToDouble(x -> x * step + start).toArray();
	}

	public ArrayList<Double> linspace(double start, double stop, int n, boolean roundToInt) {
		ArrayList<Double> result = new ArrayList<>();
		double step = (stop - start) / (n - 1);
		for (int i = 0; i <= n - 2; i++) {
			if (roundToInt) {
				BigDecimal bd = new BigDecimal(start + (i * step));
				bd = bd.setScale(0, RoundingMode.HALF_UP);
				result.add(bd.doubleValue());
			} else {
				result.add(start + (i * step));
			}
		}
		result.add(stop);
		return result;
	}

	private int checkIfValueIsInRange(double startOfRange, double endOfRange, double value) {
		if (value < startOfRange) {
			return 0;
		} else if (value >= startOfRange && value <= endOfRange) {
			return 1;
		} else {
			return 2;
		}
	}

	public int ternaryToDecimalConverter(ArrayList<Integer> ternaryNumber) {

		int decimalValue = 0;

		for (int i = 0; i < ternaryNumber.size(); i++) {
			if (ternaryNumber.get(i) >= 0 && ternaryNumber.get(i) <= 2) {
				decimalValue += ternaryNumber.get(i) * Math.pow(3, i);
			} else {
				throw new IllegalArgumentException();
			}
		}

		return decimalValue;
	}

	public void setData(double[][] data) {
		this.data = data;
	}

	public void setIsoFactor(double isoFactor) {
		this.isoFactor = isoFactor;
	}

	public void setInterpolationFactor(int interpolationFactor) {
		this.interpolationFactor = interpolationFactor;
	}

	public void setMapColorScale(String mapColorScale) {
		this.mapColorScale = mapColorScale;
	}
}
