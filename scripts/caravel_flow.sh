#! /bin/sh
set -o errexit
set -o nounset
set -o xtrace

export WRAPPER=WBWrapper
export PROJ_HOME=`pwd`
export SCRIPT_ROOT=$PROJ_HOME/scripts
./mill dma.runMain dma.$WRAPPER

export CARAVEL_PROJ_ROOT=$PROJ_HOME/caravel_user_project
if [ -d $CARAVEL_PROJ_ROOT ]; then
    cd $CARAVEL_PROJ_ROOT
else
    git clone https://github.com/efabless/caravel_user_project.git
    cd $CARAVEL_PROJ_ROOT
    make install
fi

export OPENLANE_ROOT=$PROJ_HOME/openlane
export OPENLANE_TAG="v0.15"
if [ ! -d $OPENLANE_ROOT ]; then
    make openlane
    wget https://github.com/datenlord/axi-dma/releases/download/$OPENLANE_TAG/pdks.tgz
    tar zxvf pdks.tgz --directory $OPENLANE_ROOT
fi

#make simenv
export PDK_ROOT=$OPENLANE_ROOT/pdks
export CARAVEL_ROOT=$PROJ_HOME/caravel
# specify simulation mode: RTL/GL
export SIM=RTL
# Run IO ports testbench
#make verify-io_ports

export PRECHECK_ROOT=$PROJ_HOME/precheck
# Install precheck
#make precheck
#make run-precheck

cp $PROJ_HOME/$WRAPPER.v verilog/rtl/
cp $SCRIPT_ROOT/user_proj_example.v verilog/rtl/
cp $SCRIPT_ROOT/config.tcl openlane/user_proj_example/

make user_proj_example
make user_project_wrapper
