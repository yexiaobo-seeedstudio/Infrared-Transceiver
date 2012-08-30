#ifndef _IR_DFS_H_
#define _IR_DFS_H_

#include <Arduino.h>

//#define ALL_MODE                1
#define DEBUG_MODE              0
//#define SEND_MODE               0
//#define REV_MODE                0
//#define BLUETOOTH_MODE_SEND     0
//#define BLUETOOTH_MODE_REV      0

#ifndef INT8U
#define INT8U unsigned char
#endif
#ifndef INT8S
#define INT8S signed char
#endif
#ifndef INT16U
#define INT16U unsigned int
#endif
#ifndef INT16S
#define INT16S signed int
#endif
#ifndef INT32U
#define INT32U unsigned long
#endif
#ifndef INT32S
#define INT32S signed long
#endif

// len, start_H, start_L, nshort, nlong, data_len, data[data_len]....
#define D_LEN       0
#define D_STARTH    1
#define D_STARTL    2
#define D_SHORT     3
#define D_LONG      4
#define D_DATALEN   5
#define D_DATA      6

// state machine
#define SM_WAITORDER            0x01
#define SM_STUDY                0x10
#define SM_ORDER                0x20

#endif
