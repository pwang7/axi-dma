#! /bin/sh
set -o errexit
set -o nounset
set -o xtrace

DESIGN=Dma
TAG=run-`date +"%Y-%m-%d-%H-%M-%S-%N-%Z"`

if [ -d "designs/$DESIGN" ]; then
    ./flow.tcl -design $DESIGN -init_design_config -overwrite
else
    ./flow.tcl -design $DESIGN -init_design_config -src $DESIGN.v
fi

./flow.tcl -design $DESIGN -tag $TAG |& tee > designs/$DESIGN/runs/$TAG.log
