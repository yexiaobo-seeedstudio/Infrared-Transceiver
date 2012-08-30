 #include <IRremote.h>
//#include <debug_lvc.h>
#include <SoftwareSerial.h>
#include "IR_dfs.h"
#include "IR_BlueTooth.h"
#include "IR_SendRev.h"

#define BUF_LEN 28

INT8U blueDataRevLen = 0;               // blue tooth
INT8U blueDataRevBuf[BUF_LEN];          // blue tooth

INT8U iRlen = 0;                        // ir len
INT8U pIrRev[BUF_LEN];                  // ir buf

// init blueDataRevLen and blueDataRevBuf
void initDataRev()
{
    blueDataRevLen = 0;
    memcpy(blueDataRevBuf, 0, BUF_LEN*sizeof(INT8U));
}

// 0: bad packge;   1: study mode   2: order mode
/*
    bluetooth packge format:
    {HEAD, MODE, DATA, TAIL}
    HEAD: {0x83, 0x31}
    MODE: 0x31: order mode
          0x30: study mode
    DATA: data
    TAIL: {0x2f, 0x45}
    
    STUDYMODE: no data, so the package is {0x53, 0x31, 0x30, 0x2f, 0x45};
    ORDERMODE: include ir data, so the package is {0x53, 0x30, 0x31, data...., 0x2f, 0x45};
*/
int isGoodPackge(INT8U len, INT8U * idata)
{
    // check the HEAD and TAIL
    if(idata[0] == 0x53 && idata[1] == 0x31 && idata[len-2] == 0x2f && idata[len-1] == 0x45)
    {
#if DEBUG_MODE 
        Serial.print("GOOD PACKAGE, data follows:\r\n");
        for(int i = 0; i<len; i++)
        {
            Serial.print(idata[i]);Serial.print("\t");
        }
        Serial.print("\r\n");
#endif
        return (idata[2] - 0x2f);
    }
    else    // HEAD or/and TAIL wrong
    {
#if DEBUG_MODE
    Serial.print("BAD PACKAGE, the data is:\r\n");
    for(int i = 0; i<len; i++){
        Serial.print(idata[i]);Serial.print("\t");
    }
    Serial.print("\r\n");
#endif
    }
    return 0;
}


#if DEBUG_MODE
int flag = 1;
#endif 

/*
    state machine: there are 3 state:
    SM_WAITORDER : wait for order
    SM_STUDY     : study mode, begin to study, if there is no ir data within 10s, time out, turn to SM_WAITORDER
    SM_ORDER     : order mode, get order ,send data to ir to control the ir quipment,such as TV.
*/
unsigned char state = SM_WAITORDER;     // state

int tout = 0;
#define TIMEOUT 9000                    // 10s

void stateMachine()
{
    switch(state)
    {
        //***********************************************WAIT FOR ORDER********************************
        case SM_WAITORDER:   
#if DEBUG_MODE
        if(flag)
        {
            Serial.print("WAIT FOR ORDER MODE!\r\n");
            flag = 0;
        }
#endif

        blueDataRevLen = blueToothRev(blueDataRevBuf);
#if DEBUG_MODE && 0x00
        if(blueDataRevLen > 0)
        {
            Serial.print("state = SM_WAITORDER, get some data from bluetooth!\r\n");
            for(int i = 0; i<blueDataRevLen; i++)
            {
                Serial.print(blueDataRevBuf[i])Serial.print("\t");
            }
            Serial.println();
        }
#endif
        if(blueDataRevLen > 0)
        {
            INT8U igp = isGoodPackge(blueDataRevLen, blueDataRevBuf);
            if(igp > 0)     // good packge
            {
                Serial.print("igp > 0\r\n");
                if(igp == 1)  
                {
                    state = SM_STUDY;
#if DEBUG_MODE      
                    Serial.print("GET GOOD PACKAGE!!:  STATE TURN TO STUDY MODE!\r\n");
#endif
                }
                else if(igp == 2) 
                {
                    state = SM_ORDER;
#if DEBUG_MODE      
                    Serial.print("GET GOOD PACKAGE!!:  STATE TURN TO ORDER MODE!\r\n");
#endif
                }
            }
        }

        break;
        //***********************************************STUDY MODE************************************
        case SM_STUDY:

#if DEBUG_MODE
        Serial.print("STUDY MODE:  WAIT FOR IR DATA\r\n");
#endif
        ir_clear();                // clear ir data
        ir_init();
        delay(5);
        
        while(!isIrData())         // wait for ir data, if more than 10 seconds, as time out
        {
            delay(10);
            tout++;
            if(tout == TIMEOUT)
                break;
        }
        
        if(tout == TIMEOUT)         // if time out
        {
#if DEBUG_MODE
            Serial.print("TIME OUT!! GOTO WAIT FOR ORDER MODE!\r\n");
            flag = 1;
#endif  
            ir_init();              // init ir again
            state = SM_WAITORDER;   // change state to SM_WAITORDER
            tout = 0;               // clear tout
            break;                  // break this case
        }
#if DEBUG_MODE
        Serial.print("STUDY MODE:  GET DATA FROM IR REMOTE\r\n");
#endif   

        iRlen = isIrData();         // get ir data length
        
#if DEBUG_MODE   
        Serial.print("iRlen = ");Serial.println(iRlen);
#endif 
        ir_rev(pIrRev);             // get ir data if buff: pIrRev

#if DEBUG_MODE
        Serial.print("STUDY MODE:  BEGIN TO SEND DATA TO BLUETOOTH\r\n");
#endif
        blueToothSend(iRlen, pIrRev);   // sned data to bluetooth
        
        state = SM_WAITORDER;           // change state to SM_WAITORDER
#if DEBUG_MODE
        Serial.print("STUDY MODE:  EXIT STUDY MODE, GO TO WAIT ORDER MODE\r\n");
        flag = 1;
#endif
        break;

        //***********************************************ORDER MODE************************************
        case SM_ORDER:

#if DEBUG_MODE

  /*      Serial.print("ORDER MODE:  \r\n");
        Serial.print("ORDER MODE: freq = ");Serial.println(blueDataRevBuf[3]);
        Serial.print("ORDER MODE: len = ");Serial.println(blueDataRevBuf[4]);
 
        for(int i = 0; i<blueDataRevBuf[4]; i++)
        {
            Serial.print(blueDataRevBuf[5+i]);Serial.print("\t");
        }
        Serial.print("\r\n");*/
#endif

        ir_send(&blueDataRevBuf[4], blueDataRevBuf[3]);
        initDataRev();
        state = SM_WAITORDER;
#if DEBUG_MODE
        Serial.print("ORDER MODE:  EXIT ORDER MODE, GO TO WAIT ORDER MODE\r\n");
        flag = 1;
#endif
        initDataRev();
        
        break;
        default:
            ;
    }

}

void setup()
{
#if DEBUG_MODE
    Serial.begin(115200);
    Serial.println("init.........\r\n");
#endif

    ir_init();          // ir init
    
    blueTooth_Init();   // bluetooth init
    delay(200);
    
#if DEBUG_MODE
    Serial.println("init bluetooth ok!!\r\n");
#endif

}

// loop
void loop()
{
    stateMachine();
}

