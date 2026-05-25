#!/bin/bash

# Script de lancement de VideoScramble
# Auteur: Abdoulaye DIALLO

# Couleurs pour l'affichage
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔═══════════════════════════════════════╗${NC}"
echo -e "${BLUE}║        VideoScramble - v1.0          ║${NC}"
echo -e "${BLUE}║  Chiffrement/Déchiffrement Vidéo     ║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════╝${NC}"
echo ""

# Vérifier que Java est installé
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java n'est pas installé. Veuillez installer Java 17 ou supérieur.${NC}"
    exit 1
fi

# Vérifier la version de Java
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo -e "${RED}❌ Java 17 ou supérieur est requis. Version actuelle: $JAVA_VERSION${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Java $JAVA_VERSION détecté${NC}"

# Vérifier que Maven est installé
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}❌ Maven n'est pas installé. Veuillez installer Maven 3.6 ou supérieur.${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Maven détecté${NC}"
echo ""

# Fonction d'affichage de l'aide
show_help() {
    echo -e "${YELLOW}Usage:${NC}"
    echo "  ./run.sh                           - Mode graphique (interface JavaFX)"
    echo "  ./run.sh scramble <in> <out> <key> - Chiffrer une vidéo"
    echo "  ./run.sh unscramble <in> <out> <key> - Déchiffrer une vidéo"
    echo "  ./run.sh crack <in> <out>          - Casser la clé par force brute"
    echo "  ./run.sh scramble-embed <in> <out> <key> - Chiffrer avec embarquement de clé"
    echo "  ./run.sh unscramble-extract <in> <out> - Déchiffrer avec extraction de clé"
    echo ""
    echo -e "${YELLOW}Exemples:${NC}"
    echo "  ./run.sh scramble video.mp4 chiffree.mp4 12345"
    echo "  ./run.sh unscramble chiffree.mp4 dechiffree.mp4 12345"
    echo "  ./run.sh crack chiffree.mp4 cassee.mp4"
    echo ""
    echo -e "${YELLOW}Options:${NC}"
    echo "  --help, -h      - Afficher cette aide"
    echo "  --compile, -c   - Compiler le projet avant de lancer"
    echo ""
}

# Parser les options
COMPILE=false
if [[ "$1" == "--help" ]] || [[ "$1" == "-h" ]]; then
    show_help
    exit 0
fi

if [[ "$1" == "--compile" ]] || [[ "$1" == "-c" ]]; then
    COMPILE=true
    shift
fi

# Compiler si demandé ou si target n'existe pas
if [ "$COMPILE" = true ] || [ ! -d "target" ]; then
    echo -e "${BLUE}🔨 Compilation du projet...${NC}"
    mvn clean compile
    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ Erreur de compilation${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Compilation réussie${NC}"
    echo ""
fi

# Traiter les arguments
if [ $# -eq 0 ]; then
    # Mode graphique
    echo -e "${BLUE}🚀 Lancement en mode graphique...${NC}"
    mvn javafx:run
elif [ "$1" == "scramble" ]; then
    if [ $# -ne 4 ]; then
        echo -e "${RED}❌ Usage: ./run.sh scramble <input> <output> <key>${NC}"
        exit 1
    fi
    echo -e "${BLUE}🔒 Chiffrement de $2 avec la clé $4...${NC}"
    mvn javafx:run -Djavafx.args="scramble $2 $3 $4 false"
elif [ "$1" == "unscramble" ]; then
    if [ $# -ne 4 ]; then
        echo -e "${RED}❌ Usage: ./run.sh unscramble <input> <output> <key>${NC}"
        exit 1
    fi
    echo -e "${BLUE}🔓 Déchiffrement de $2 avec la clé $4...${NC}"
    mvn javafx:run -Djavafx.args="unscramble $2 $3 $4 false"
elif [ "$1" == "crack" ]; then
    if [ $# -ne 3 ]; then
        echo -e "${RED}❌ Usage: ./run.sh crack <input> <output>${NC}"
        exit 1
    fi
    echo -e "${BLUE}🔨 Cassage de la clé de $2...${NC}"
    echo -e "${YELLOW}⚠️  Cela peut prendre plusieurs minutes...${NC}"
    mvn javafx:run -Djavafx.args="crack $2 $3 -1 false"
elif [ "$1" == "scramble-embed" ]; then
    if [ $# -ne 4 ]; then
        echo -e "${RED}❌ Usage: ./run.sh scramble-embed <input> <output> <key>${NC}"
        exit 1
    fi
    echo -e "${BLUE}🔒🔑 Chiffrement de $2 avec embarquement de la clé $4...${NC}"
    mvn javafx:run -Djavafx.args="scramble $2 $3 $4 true"
elif [ "$1" == "unscramble-extract" ]; then
    if [ $# -ne 3 ]; then
        echo -e "${RED}❌ Usage: ./run.sh unscramble-extract <input> <output>${NC}"
        exit 1
    fi
    echo -e "${BLUE}🔓🔑 Déchiffrement de $2 avec extraction de la clé...${NC}"
    mvn javafx:run -Djavafx.args="unscramble $2 $3 0 true"
else
    echo -e "${RED}❌ Commande inconnue: $1${NC}"
    echo ""
    show_help
    exit 1
fi
