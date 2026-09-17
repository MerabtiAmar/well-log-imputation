# Reconstruction de diagraphies manquantes par machine learning

Application de bureau réalisée pour mon **projet de fin d'études de Licence** (USTHB, 2023), pendant un stage chez **ENAGEO** (groupe Sonatrach). Des géophysiciens y importent des diagraphies de puits (mesures indexées par la profondeur), entraînent des modèles et **complètent les mesures manquantes**, sans écrire de code.

> Les données de puits de l'entreprise ne sont pas publiées. Le dossier `data/` contient des **diagraphies synthétiques** au même format, générées pour tester l'application.

## Architecture

```
Interface Java Swing (app/src)
 ├─ connexion : comptes MySQL avec rôles admin / utilisateur
 ├─ espace admin : création, modification et suppression d'utilisateurs
 └─ espace utilisateur
     ├─ Entraînement : import CSV → grille éditable → export → python train.py <algorithme>
     └─ Prédiction   : import CSV avec trous → python predict.py <algorithme> → tableau complété → export
```

- **`train.py`** entraîne, pour chaque mesure (colonnes après `DEPTH`), un modèle qui la prédit à partir des autres mesures. Cinq algorithmes sont proposés : régression linéaire, SVM à noyau RBF, perceptron multicouche 100/50 ReLU, forêt aléatoire, gradient boosting. Les modèles sont sauvegardés en `.pkl`.
- **`predict.py`** parcourt les lignes et remplit chaque valeur manquante avec le modèle de la colonne correspondante.
- Java et Python échangent via des fichiers CSV temporaires ; les scripts tournent en arrière-plan (`SwingWorker`) sans figer l'interface.

## Résultats du mémoire

Prédiction de paramètres de roche réservoir à partir des diagraphies de 3 puits, en **généralisation inter-puits** (évaluation sur un puits jamais vu) :

| Modèle | Corrélation (r) |
|---|---:|
| Régression linéaire | 0,15 |
| SVM (RBF) | 0,29 |
| MLP 100/50 ReLU | **0,52** (MAE 21,4, RMSE 27,6) |

La corrélation reste insuffisante sur des formations géologiques hétérogènes.

## Suite : CDD chez ENAGEO (2023–2024)

J'ai poursuivi ce travail pendant un an de CDD à temps partiel chez ENAGEO. Chaque caractéristique de la roche (élasticité, densité…) est mesurée à intervalles réguliers le long du puits. Les séries obtenues sont souvent incomplètes (matériel défaillant, données perdues au transfert ou supprimées), et ces mesures coûtent très cher. L'objectif était de **prédire la série elle-même** là où elle manque, en traitant chaque série indexée par la profondeur comme une série temporelle (tendance, saisonnalité, cycles), avec des modèles de deep learning, principalement des LSTM et des RNN :

1. **Imputation dans un même puits** : prédire les valeurs manquantes d'une série à partir de ses parties mesurées. Avec les LSTM : **MAE 12,7, RMSE 15,2**.
2. **Généralisation inter-puits** : entraîner un LSTM sur la série d'un puits et prédire la même caractéristique dans un autre puits, dans la continuité du stage. Par rapport au stage, la corrélation est passée de **0,52 à 0,82**, la MAE de **21,4 à 15,8** et la RMSE de **27,6 à 20,2**, soit 20 à 30 % d'erreur en moins.

Ces résultats ne suffisaient pas encore pour une exploitation par l'entreprise ; le projet était toujours en développement à mon départ en France. Le code et les données du CDD, propriété d'ENAGEO, ne sont pas publiés ici.

## Lancer l'application

Prérequis : Java 17+, Python 3.10+, MySQL 8 et le [pilote MySQL Connector/J](https://dev.mysql.com/downloads/connector/j/).

```bash
# 1. Base de données (crée aussi un compte admin/admin à changer)
mysql -u root -p < sql/schema.sql

# 2. Dépendances Python
pip install -r app/requirements.txt

# 3. Compilation et lancement, depuis app/ (les scripts Python y sont appelés)
cd app
javac -encoding UTF-8 -d out src/*.java
java -cp "out;mysql-connector-j-8.0.33.jar" EnageoPrediction      # Linux/macOS : out:mysql-connector-j-8.0.33.jar
```

Connexion à la base et interpréteur Python se configurent par variables d'environnement :

| Variable | Défaut |
|---|---|
| `ENAGEO_DB_URL` | `jdbc:mysql://localhost:3306/enageo` |
| `ENAGEO_DB_USER` / `ENAGEO_DB_PASSWORD` | `root` / *(vide)* |
| `ENAGEO_PYTHON` | `python` |

Pour tester : se connecter, importer `data/synthetic_train.csv` dans l'onglet d'entraînement et lancer l'entraînement, puis importer `data/synthetic_predict.csv` (30 % de valeurs `C` manquantes) dans l'onglet de prédiction.

Sans l'interface, les scripts se testent seuls :

```bash
cd app
cp ../data/synthetic_train.csv temporary_csv_train.csv
cp ../data/synthetic_predict.csv temporary_csv_predict.csv
python train.py "Random Forest"
python predict.py "Random Forest"    # écrit updated_dataset.csv
```

## Contenu

```
app/src/        interface Swing (Login, MainApp, AppConfig)
app/*.py        entraînement et prédiction (scikit-learn)
sql/schema.sql  table des utilisateurs
data/           diagraphies synthétiques (DEPTH, A, B, C)
research/       prototypes de comparaison des modèles utilisés pendant le stage (données non fournies)
```

## Limites

- Les mots de passe sont stockés en clair dans la base, comme dans la version livrée en 2023 ; un hachage (bcrypt, PBKDF2) serait nécessaire en production.
- L'évaluation n'est pas intégrée à l'interface : elle a été menée dans les scripts de `research/`.

**Modifications par rapport à la version livrée :**
- identifiants de base et chemin de Python rendus configurables ;
- logo de l'entreprise remplacé par une bannière neutre ;
- ajout du schéma SQL sans comptes réels et des données synthétiques.

## Stack

Java (Swing, JDBC) · MySQL · Python (pandas, scikit-learn)

## Licence

Code distribué sous [licence MIT](LICENSE).

## Auteur

**Amar Merabti** — Licence informatique, USTHB ; stage (févr.–juin 2023) puis CDD (juil. 2023–juil. 2024) chez ENAGEO.
