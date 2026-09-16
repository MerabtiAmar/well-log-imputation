import csv
import pickle
import sys
import time
import pandas as pd
from sklearn.model_selection import GridSearchCV
from sklearn.neural_network import MLPRegressor
from tqdm import tqdm


def extract_first_row(file_csv):
    with open(file_csv, 'r') as file_csv:
        csv_reader = csv.reader(file_csv)
        first_row = next(csv_reader)
        return ','.join(first_row)


csv_file = "p1p2_sorted.csv"
data = pd.read_csv(csv_file)
characteristics = extract_first_row(csv_file)
numeric_columns = characteristics.split(',')[1:]
data = data[numeric_columns]

trained_models_nn = dict()

X = data.drop('C', axis=1)
y = data['C']

# Define the parameter grid for grid search
parameter_grid = {
    'hidden_layer_sizes': [(100,), (500,), (1000,)],
    'activation': ['relu', 'tanh']
}

# Create the MLPRegressor model
mlp_model = MLPRegressor()

# Perform grid search with cross-validation
grid_search = GridSearchCV(mlp_model, parameter_grid, cv=3, refit=False)

# Get the total number of parameter combinations
total_combinations = len(grid_search.param_grid['hidden_layer_sizes']) * len(grid_search.param_grid['activation'])
progress_bar = tqdm(total=total_combinations, desc="Progress")

start_time = time.time()

for params in grid_search.param_grid['hidden_layer_sizes']:
    for activation in grid_search.param_grid['activation']:
        # Create a new MLPRegressor model with the current parameters
        mlp_model = MLPRegressor(hidden_layer_sizes=params, activation=activation)

        grid_search.estimator = mlp_model
        grid_search.fit(X, y)

        # Update progress bar
        progress_bar.update(1)

        # Get the best model from the grid search
        best_model = grid_search.best_estimator_
        trained_models_nn[f"C_{params}_{activation}"] = best_model

end_time = time.time()
progress_bar.close()

print(f"Neural Networks training time for C: {end_time - start_time:.2f} seconds")

model_file = 'trained_models_nn.pkl'
with open(model_file, 'wb') as file:
    pickle.dump(trained_models_nn, file)
