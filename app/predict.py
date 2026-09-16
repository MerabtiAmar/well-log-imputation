import csv
import pandas as pd
import pickle
import sys


def extract_first_row(file_csv):
    with open(file_csv, 'r') as file_csv:
        csv_reader = csv.reader(file_csv)
        first_row = next(csv_reader)
        return ','.join(first_row)


algorithm = sys.argv[1]  # Get the algorithm from command-line argument
model_paths = {
    "Regression": "trained_models_regression.pkl",
    "Neural Networks": "trained_models_nn.pkl",
    "Support Vector Machine": "trained_models_svm.pkl",
    "Random Forest": "trained_models_rf.pkl",
    "Gradient Boosting": "trained_models_gb.pkl",
}

# Check if the provided algorithm is valid
if algorithm not in model_paths.keys():
    print("Invalid algorithm specified!")
    sys.exit(2)

# Load the corresponding trained models
model_path = model_paths[algorithm]
with open(model_path, 'rb') as f:
    trained_models = pickle.load(f)

# Load the dataset from CSV file
dataset = pd.read_csv("temporary_csv_predict.csv")
numeric = extract_first_row("temporary_csv_predict.csv")
numeric = numeric.split(',')
first = numeric[0]
ids = dataset[first]
ids = ids.tolist()
numeric = numeric[1:]
dataset = dataset[numeric]
# Iterate through the rows and predict missing values
for index, row in dataset.iterrows():
    missing_features = row[row.isnull()].index.tolist()
    for feature_name in missing_features:
        if feature_name in trained_models:
            model = trained_models[feature_name]
            row_copy = row.drop(feature_name).copy().to_frame().transpose()
            predicted_value = model.predict(row_copy)
            dataset.at[index, feature_name] = predicted_value
        else:
            exit(-1)

# Print the resulting dataset
dataset.insert(0, first, ids)
dataset.to_csv("updated_dataset.csv", index=False)
