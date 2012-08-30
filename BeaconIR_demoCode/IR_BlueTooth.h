#ifndef _IR_BLUETOOTH_H_
#define _IR_BLUETOOTH_H_

#include <SoftwareSerial.h>   //Software Serial Port

static void setupBlueToothConnection();
static void CheckOK();
static void sendBlueToothCommand(char command[]);
static void checkBluetoothState(char state);


void blueTooth_Init();

// send data to bluetooth, len: data length, dataSend: data buffer
void blueToothSend(INT8U len, INT8U *dataSend); 

// receive data from bluetooth,  dataRev: data buffer    
INT8U blueToothRev(INT8U *dataRev);


#endif
