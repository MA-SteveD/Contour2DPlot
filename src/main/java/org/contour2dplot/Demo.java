package org.contour2dplot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

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

public class Demo extends Application {

	private static final String TAB = "\t";

	private static final Logger LOGGER = LoggerFactory.getLogger(Demo.class);

	private Parent createContent() {

		BorderPane borderPane = new BorderPane();

		Contour2DMap contour2DMap = new Contour2DMap(400, 200);

		contour2DMap.setMinSize(400, 200);
		contour2DMap.setPrefSize(400, 200);
		contour2DMap.setMaxSize(400, 200);

		double[][] data = loadData(new File("RandomData.txt"));
		contour2DMap.setData(data);
		contour2DMap.setIsoFactor(0.1);
		contour2DMap.setInterpolationFactor(3);
		contour2DMap.setMapColorScale("Color");
		contour2DMap.draw();

		borderPane.setCenter(contour2DMap);

		return borderPane;
	}

	@Override
	public void start(Stage stage) throws Exception {
		stage.setScene(new Scene(createContent()));
		stage.setWidth(500);
		stage.setHeight(300);
		stage.show();
	}

	public static void main(String[] args) {
		launch(args);
	}

	private double[][] loadData(File file) {
		return loadData(file, TAB);
	}

	private double[][] loadData(File file, String delimter) {

		ArrayList<ArrayList<Double>> rawData = new ArrayList<>();

		try {
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			String line;

			while ((line = bufferedReader.readLine()) != null) {
				String[] values = line.split(delimter);
				ArrayList<Double> row = new ArrayList<>();
				for (String value : values) {
					row.add(Double.valueOf(value));
				}
				rawData.add(row);
			}
			bufferedReader.close();

			LOGGER.debug("Loaded data from file {}:\n{}", file.getCanonicalPath(), rawData);

		} catch (IOException ex) {
			ex.printStackTrace();
		}

		double[][] data = new double[rawData.size()][rawData.get(0).size()];

		for (int i = 0; i < data.length; i++) {
			for (int j = 0; j < data[i].length; j++) {
				data[i][j] = rawData.get(i).get(j);
			}
		}

		return data;
	}
}
