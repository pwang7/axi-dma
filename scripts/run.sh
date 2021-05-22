#! /bin/sh

set -o errexit
set -o nounset
set -o xtrace

MILL_VERSION=0.9.7

if [ ! -f mill ]; then
  curl -L https://github.com/com-lihaoyi/mill/releases/download/$MILL_VERSION/$MILL_VERSION > mill && chmod +x mill
fi

./mill version

# Run test and simulation
./mill dma.runMain dma.DmaSim
./mill dma.runMain dma.DmaMem
./mill dma.test

# Check format
./mill dma.checkFormat
#./mill dma.fix --check

