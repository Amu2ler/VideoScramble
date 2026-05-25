# VideoScramble - Chiffrement/Déchiffrement Vidéo

Application JavaFX pour le chiffrement et le déchiffrement de vidéos par permutation de lignes.

**Auteurs** : Arthur MULLER & Abdoulaye DIALLO

## Description

VideoScramble implémente un système de chiffrement vidéo basé sur la permutation des lignes d'images. Ce principe, inspiré des systèmes de chiffrement TV des années 80/90, permet de rendre une vidéo illisible tout en maintenant des performances de traitement acceptables.

### Principe de chiffrement

- **Clé de chiffrement** : 15 bits (0 à 32767)
  - 8 bits pour l'offset (r)
  - 7 bits pour le step (s)
- **Formule de permutation** : Une ligne d'indice `i` est placée en position `(r + (2s+1) * i) % size`
- **Traitement par blocs** : Les lignes sont mélangées par itérations sur des puissances de 2

### Fonctionnalités

✅ **Étape 1 : Chiffrement/Déchiffrement**
- Chiffrement de vidéo avec une clé donnée
- Déchiffrement de vidéo avec la clé
- Visualisation simultanée des vidéos d'entrée et de sortie
- Affichage de la clé et des informations de traitement
- Support de l'embarquement de la clé dans le pixel (0,0)

✅ **Étape 2 : Cassage de clé**
- Recherche par force brute (32768 clés possibles)
- Critères de sélection :
  - Distance euclidienne
  - Corrélation de Pearson
- Affichage de la progression du cassage

✅ **Étape 3 : Clé embarquée**
- Embarquement de la clé dans les 15 bits de poids faible du pixel (0,0)
- Extraction automatique de la clé lors du déchiffrement
- Support des clés changeantes image par image

## Installation

### Prérequis

- Java 17 ou supérieur
- Maven 3.6 ou supérieur
- JavaFX 17
- OpenCV 4.9

### Compilation

```bash
mvn clean compile
```

### Exécution

#### Mode graphique (avec interface JavaFX)

```bash
mvn javafx:run
```

#### Mode ligne de commande

```bash
mvn javafx:run -Djavafx.args="<mode> <input> <output> <key> <embedKey>"
```

**Paramètres :**
- `mode` : "scramble" (chiffrer) ou "unscramble" (déchiffrer) ou "crack" (casser la clé)
- `input` : Chemin vers la vidéo d'entrée
- `output` : Chemin vers la vidéo de sortie
- `key` : Clé de chiffrement (0-32767), ou -1 pour cassage automatique
- `embedKey` : "true" ou "false" pour embarquer la clé dans l'image

### Exemples d'utilisation

#### 1. Chiffrer une vidéo avec la clé 12345

```bash
mvn javafx:run -Djavafx.args="scramble input.mp4 output_scrambled.mp4 12345 false"
```

#### 2. Déchiffrer une vidéo avec la clé 12345

```bash
mvn javafx:run -Djavafx.args="unscramble output_scrambled.mp4 output_unscrambled.mp4 12345 false"
```

#### 3. Chiffrer avec embarquement de la clé

```bash
mvn javafx:run -Djavafx.args="scramble input.mp4 output_scrambled.mp4 12345 true"
```

#### 4. Déchiffrer avec extraction automatique de la clé

```bash
mvn javafx:run -Djavafx.args="unscramble output_scrambled.mp4 output_unscrambled.mp4 0 true"
```

#### 5. Casser la clé par force brute

```bash
mvn javafx:run -Djavafx.args="crack output_scrambled.mp4 output_unscrambled.mp4 -1 false"
```

## Interface graphique

L'interface JavaFX permet de :

- **Visualiser** les vidéos d'entrée et de sortie côte à côte en temps réel
- **Sélectionner** les fichiers vidéo via des boîtes de dialogue
- **Suivre** la progression du traitement avec une barre de progression
- **Afficher** les informations sur la clé utilisée
- **Monitorer** les performances (FPS, nombre de frames, résolution)

### Utilisation de l'interface

1. Cliquer sur "Sélectionner vidéo d'entrée" pour choisir la vidéo à traiter
2. Cliquer sur "Sélectionner vidéo de sortie" pour définir le fichier de sortie
3. Cliquer sur "Démarrer le traitement" pour lancer le chiffrement/déchiffrement

## Architecture du code

### Classes principales

- **`VideoScrambleApplication`** : Point d'entrée de l'application
- **`VideoScrambleController`** : Contrôleur JavaFX pour l'interface
- **`ScrambleEngine`** : Moteur de chiffrement/déchiffrement
  - Méthodes `scramble()` et `unscramble()`
  - Gestion de l'embarquement/extraction de la clé
- **`KeyCracker`** : Cassage de clé par force brute
  - Support des callbacks de progression
- **`SimilarityCriterion`** : Interface pour les critères de similarité
- **`EuclideanCriterion`** : Critère basé sur la distance euclidienne
- **`PearsonCriterion`** : Critère basé sur la corrélation de Pearson

### Diagramme de flux

```
Vidéo d'entrée
     ↓
ScrambleEngine.scramble() / unscramble()
     ↓
Traitement par blocs (puissances de 2)
     ↓
Embarquement de la clé (optionnel)
     ↓
Vidéo de sortie
```

## Performances

Sur une machine moderne :

- **HD 1080p (1920×1080)** : ~30-60 fps
- **HD 720p (1280×720)** : ~60-90 fps
- **SD (640×360)** : ~100-150 fps

**Cassage de clé** (32768 clés) :
- ~2-5 secondes pour une image 640×360
- ~10-30 secondes pour une image 1920×1080

## Limitations et améliorations futures

### Limitations connues

- Le cassage de clé n'est pas en temps réel pour les vidéos HD
- La compression vidéo peut altérer la clé embarquée
- Le chiffrement ne traite que la partie vidéo (pas d'audio)

### Améliorations possibles

1. **Codecs sans perte** : Utiliser FFV1 pour préserver la clé embarquée
2. **Code correcteur d'erreurs** : Protéger la clé contre la compression
3. **Redondance** : Embarquer la clé plusieurs fois avec vote majoritaire
4. **Espace YUV** : Embarquer la clé dans le canal Y (moins affecté par compression)
5. **Parallélisation** : Utiliser plusieurs threads pour le cassage de clé
6. **GPU** : Exploiter OpenCL/CUDA pour accélérer le traitement

## Tests et validation

Pour valider le bon fonctionnement :

1. Chiffrer une vidéo avec une clé connue
2. Déchiffrer avec la même clé → la vidéo doit être identique à l'originale
3. Tester le cassage de clé sur une image chiffrée
4. Vérifier l'embarquement/extraction de la clé

## Références

- Systèmes de chiffrement TV des années 80/90
- OpenCV Documentation : https://docs.opencv.org/
- JavaFX Documentation : https://openjfx.io/

## Licence

Projet académique - Tous droits réservés

---

**Note** : Ce projet est réalisé dans un cadre pédagogique. Il ne doit pas être utilisé pour des applications de sécurité réelles sans une analyse approfondie de ses propriétés cryptographiques.
