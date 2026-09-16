import csv
import pickle
from sklearn.svm import SVR
from sklearn.neural_network import MLPRegressor
from sklearn.linear_model import LinearRegression
from sklearn.ensemble import RandomForestRegressor, GradientBoostingRegressor
import pandas as pd
import sys
import time


def extract_first_row(file_csv):
    with open(file_csv, 'r') as file_csv:
        csv_reader = csv.reader(file_csv)
        first_row = next(csv_reader)
        return ','.join(first_row)


if len(sys.argv) < 2:
    print("Please provide the algorithm type")
    sys.exit(1)
algorithm_type = sys.argv[1]
csv_file = "temporary_csv_train.csv"
data = pd.read_csv(csv_file)
characteristics = extract_first_row(csv_file)
numeric_columns = characteristics.split(',')[1:]
data = data[numeric_columns]

trained_models_regression = dict()
trained_models_nn = dict()
trained_models_svm = dict()
trained_models_rf = dict()
trained_models_gb = dict()
trained_models_knn = dict()


if algorithm_type == "Regression":
    for numeric_column in numeric_columns:
        X = data.drop(numeric_column, axis=1)
        y = data[numeric_column]
        start_time = time.time()
        lr_model = LinearRegression()
        lr_model.fit(X, y)
        end_time = time.time()
        trained_models_regression[numeric_column] = lr_model
        print(f"Linear Regression training time for {numeric_column}: {end_time - start_time:.2f} seconds")
    model_file = 'trained_models_regression.pkl'
    with open(model_file, 'wb') as file:
        pickle.dump(trained_models_regression, file)

elif algorithm_type == "Support Vector Machine":
    for numeric_column in numeric_columns:
        X = data.drop(numeric_column, axis=1)
        y = data[numeric_column]
        start_time = time.time()
        svr_model = SVR(kernel='rbf')
        svr_model.fit(X, y)
        end_time = time.time()
        trained_models_svm[numeric_column] = svr_model
        print(f"Support Vector Machine training time for {numeric_column}: {end_time - start_time:.2f} seconds")
    model_file = 'trained_models_svm.pkl'
    with open(model_file, 'wb') as file:
        pickle.dump(trained_models_svm, file)

elif algorithm_type == "Neural Networks":
    for numeric_column in numeric_columns:
        X = data.drop(numeric_column, axis=1)
        y = data[numeric_column]
        start_time = time.time()
        mlp_model = MLPRegressor(hidden_layer_sizes=(100,50), activation='relu')
        mlp_model.fit(X, y)
        end_time = time.time()
        trained_models_nn[numeric_column] = mlp_model
        print(f"Neural Networks training time for {numeric_column}: {end_time - start_time:.2f} seconds")
    model_file = 'trained_models_nn.pkl'
    with open(model_file, 'wb') as file:
        pickle.dump(trained_models_nn, file)

elif algorithm_type == "Random Forest":
    for numeric_column in numeric_columns:
        X = data.drop(numeric_column, axis=1)
        y = data[numeric_column]
        start_time = time.time()
        rf_model = RandomForestRegressor(n_estimators=100, random_state=42)
        rf_model.fit(X, y)
        end_time = time.time()
        trained_models_rf[numeric_column] = rf_model
        print(f"Random Forest training time for {numeric_column}: {end_time - start_time:.2f} seconds")
    model_file = 'trained_models_rf.pkl'
    with open(model_file, 'wb') as file:
        pickle.dump(trained_models_rf, file)

elif algorithm_type == "Gradient Boosting":
    for numeric_column in numeric_columns:
        X = data.drop(numeric_column, axis=1)
        y = data[numeric_column]
        start_time = time.time()
        gb_model = GradientBoostingRegressor(n_estimators=100, random_state=42)
        gb_model.fit(X, y)
        end_time = time.time()
        trained_models_gb[numeric_column] = gb_model
        print(f"Gradient Boosting training time for {numeric_column}: {end_time - start_time:.2f} seconds")
    model_file = 'trained_models_gb.pkl'
    with open(model_file, 'wb') as file:
        pickle.dump(trained_models_gb, file)
