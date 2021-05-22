#! /bin/sh
set -o errexit
set -o nounset
set -o xtrace

export WRAPPER=WBWrapper
export PROJ_HOME=`pwd`
export SCRIPT_ROOT=$PROJ_HOME/scripts
./mill dma.runMain dma.$WRAPPER

export CARAVEL_USER_PROJ_ROOT=$PROJ_HOME/caravel_user_project
if [ -d $CARAVEL_USER_PROJ_ROOT ]; then
    cd $CARAVEL_USER_PROJ_ROOT
else
    git clone https://github.com/efabless/caravel_user_project.git
    cd $CARAVEL_USER_PROJ_ROOT
    make install
fi

export OPENLANE_ROOT=$PROJ_HOME/openlane
export OPENLANE_TAG="v0.17"
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

# Copy to Caravel user project
export DESIGN_NAME=user_proj_example
export CARAVEL_USER_DESIGN_DIR=$CARAVEL_USER_PROJ_ROOT/openlane/$DESIGN_NAME
export CARAVEL_USER_DESIGN_SRC_DIR=$CARAVEL_USER_DESIGN_DIR/src
export CARAVEL_USER_DESIGN_RTL_DIR=$CARAVEL_USER_PROJ_ROOT/verilog/rtl
cp $SCRIPT_ROOT/config.tcl $CARAVEL_USER_DESIGN_DIR
cp $PROJ_HOME/$WRAPPER.v $CARAVEL_USER_DESIGN_RTL_DIR
cp $SCRIPT_ROOT/$DESIGN_NAME.v $CARAVEL_USER_DESIGN_RTL_DIR
mkdir -p $CARAVEL_USER_DESIGN_SRC_DIR
#cp $SCRIPT_ROOT/config.tcl $CARAVEL_USER_DESIGN_DIR
cp $PROJ_HOME/$WRAPPER.v $CARAVEL_USER_DESIGN_SRC_DIR
cp $SCRIPT_ROOT/$DESIGN_NAME.v $CARAVEL_USER_DESIGN_SRC_DIR
cp $CARAVEL_USER_PROJ_ROOT/caravel/verilog/rtl/defines.v $CARAVEL_USER_DESIGN_SRC_DIR

# Copy to OpenLANE designs
export OPENLANE_USER_DESIGN_DIR=$OPENLANE_ROOT/designs/$DESIGN_NAME
export OPENLANE_USER_DESIGN_SRC_DIR=$OPENLANE_USER_DESIGN_DIR/src
mkdir -p $OPENLANE_USER_DESIGN_SRC_DIR
cp $CARAVEL_USER_PROJ_ROOT/caravel/verilog/rtl/defines.v $OPENLANE_USER_DESIGN_SRC_DIR
cp $PROJ_HOME/$WRAPPER.v $OPENLANE_USER_DESIGN_SRC_DIR
cp $SCRIPT_ROOT/$DESIGN_NAME.v $OPENLANE_USER_DESIGN_SRC_DIR
cp $SCRIPT_ROOT/config.tcl $OPENLANE_USER_DESIGN_DIR
cp $SCRIPT_ROOT/regression.cfg $OPENLANE_USER_DESIGN_DIR
cp $CARAVEL_USER_DESIGN_DIR/pin_order.cfg $OPENLANE_USER_DESIGN_DIR

make $DESIGN_NAME
make user_project_wrapper
