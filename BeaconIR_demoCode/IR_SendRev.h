#ifndef _IR_SENDREV_H_
#define _IR_SENDREV_H_

#include <IRremote.h>
#include "IR_dfs.h"

void ir_init(void);
void ir_send(INT8U *idata, INT8U ifreq);
INT8U ir_rev(INT8U *revData);
INT8U isIrData();
void ir_clear();

#endif