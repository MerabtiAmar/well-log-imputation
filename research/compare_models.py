from sklearn.svm import SVR
from sklearn.neural_network import MLPRegressor
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_absolute_error
import pandas as pd
from sklearn.model_selection import train_test_split
import matplotlib.pyplot as plt
from sklearn.preprocessing import MinMaxScaler

# Read the data and select the relevant columns
data = pd.read_csv('p1p2.csv')
numeric_columns = ['A','B','C']
data = data[numeric_columns]

# Split the data into input features (X) and target variable (y)
X = data.drop('C', axis=1)  # Input features (remove the target column)
y = data['C']  # Target variable

# Split the data into training and test sets
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# Create a scaler object and fit it on the training data
scaler = MinMaxScaler()
X_train_scaled = scaler.fit_transform(X_train)
X_test_scaled = scaler.transform(X_test)

# Create the models
mlp_model = MLPRegressor(hidden_layer_sizes=(100,50), activation='relu')
svr_model = SVR(kernel='rbf')
lr_model = LinearRegression()

# Fit the models
mlp_model.fit(X_train_scaled, y_train)
svr_model.fit(X_train_scaled, y_train)
lr_model.fit(X_train_scaled, y_train)

# Predict the target variable for test data
y_pred_mlp = mlp_model.predict(X_test_scaled)
y_pred_svr = svr_model.predict(X_test_scaled)
y_pred_lr = lr_model.predict(X_test_scaled)

# Calculate mean absolute error
mae_mlp = mean_absolute_error(y_test, y_pred_mlp)
mae_svr = mean_absolute_error(y_test, y_pred_svr)
mae_lr = mean_absolute_error(y_test, y_pred_lr)

# Calculate correlation
corr_mlp = data.corr()['A']['C']
corr_svr = data.corr()['A']['C']
corr_lr = data.corr()['A']['C']

corr_mlp1 = data.corr()['B']['C']
corr_svr1 = data.corr()['B']['C']
corr_lr1 = data.corr()['B']['C']
"""
# Visualize the results for each characteristic
for column in X_test.columns:
    plt.figure()
    plt.scatter(X_test[column], y_test, color='blue', label='Actual')
    plt.scatter(X_test[column], y_pred_mlp, color='red', label='MLPRegressor')
    plt.scatter(X_test[column], y_pred_svr, color='green', label='SVR')
    plt.scatter(X_test[column], y_pred_lr, color='orange', label='Linear Regression')
    plt.xlabel(column)
    plt.ylabel('classification')
    plt.title(f'Prediction vs Actual Values for {column}')
    plt.legend()

# Show the plots
plt.show()
"""
plt.figure()
plt.scatter(y_test, y_pred_mlp, color='red', label='MLPRegressor')
plt.scatter(y_test, y_pred_svr, color='green', label='SVR')
plt.scatter(y_test, y_pred_lr, color='orange', label='Linear Regression')
plt.title(f'Prediction vs Actual Values')
plt.legend()
plt.show()

# Print the mean absolute error and correlation for each model
print("Mean Absolute Error (MLPRegressor):", mae_mlp)
print("Mean Absolute Error (SVR):", mae_svr)
print("Mean Absolute Error (Linear Regression):", mae_lr)
print("Correlation (MLPRegressor):", corr_mlp)
print("Correlation (SVR):", corr_svr)
print("Correlation (Linear Regression):", corr_lr)
print("Correlation (MLPRegressor):", corr_mlp1)
print("Correlation (SVR):", corr_svr1)
print("Correlation (Linear Regression):", corr_lr1)
