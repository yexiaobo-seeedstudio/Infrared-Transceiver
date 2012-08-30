#include "IR_dfs.h"
#include "IR_Sendrev.h"
#include <stdlib.h>
//#include "debug_lvc.h"

int RECV_PIN = A4;

IRrecv irrecv(RECV_PIN);

IRsend irsend;

decode_results results;


// clear ir buf
void ir_clear()
{
    irrecv.resume();
}

// init ir
void ir_init(void)
{
#if REV_MODE || ALL_MODE
    irrecv.enableIRIn(); // Start the receiver
    delay(20);
    irrecv.resume();

#endif
}

// len, start_H, start_L, nshort, nlong, data_len, data[data_len]....
void ir_send(INT8U *idata, INT8U ifreq)
{
    int len = idata[0];
    INT8U start_high    = idata[1];
    INT8U start_low     = idata[2];
    INT8U nshort        = idata[3];
    INT8U nlong         = idata[4];
    INT8U datalen       = idata[5];

    INT16U *pSt = (INT16U *)malloc((4+datalen*16)*sizeof(INT16U));

    if(NULL == pSt)
    {
//#if DEBUG_MODE
//        Serial.println("not enough place!!\r\n");
//#endif
        exit(1);
    }

//#if DEBUG_MODE
//    Serial.println("begin to send ir:\r\n");
//    Serial.print("ifreq = ");Serial.println(ifreq);
//    Serial.print("len = ");Serial.println(len);
//    Serial.print("start_high = ");Serial.println(start_high);
//    Serial.print("start_low = ");Serial.println(start_low);
//    Serial.print("nshort = ");Serial.println(nshort);
//    Serial.print("nlong = ");Serial.println(nlong);
//    Serial.print("datalen = ");Serial.println(datalen);
//#endif

    pSt[0] = start_high*50;
    pSt[1] = start_low*50;

    for(int i = 0; i<datalen; i++)
    {
        for(int j = 0; j<8; j++)
        {
            if(idata[6+i] & 0x01<<(7-j))
            {
                pSt[16*i + 2*j + 2] = nshort*50;
                pSt[16*i + 2*j+3]   = nlong*50;
            }
            else
            {
                pSt[16*i + 2*j+2]   = nshort*50;
                pSt[16*i + 2*j+3]   = nshort*50;
            }
        }
    }

    pSt[2+datalen*16]   = nshort*50;
    pSt[2+datalen*16+1] = nshort*50;

//#if DEBUG_MODE
//    for(int i = 0; i<4+datalen*16; i++)
//    {
//        Serial.print(pSt[i]);Serial.print("\t");
//    }
//    Serial.println();
//#endif
    irsend.sendRaw(pSt, 4+datalen*16, ifreq);
    free(pSt);
    
}


// len, start_H, start_L, nshort, nlong, data_len, data[data_len]....
INT8U ir_rev(INT8U *revData)
{
    int count       = results.rawlen;
    int nshort      = 0;
    int nlong       = 0;
    int count_data  = 0;

    count_data = (count-4)/16;

    for(int i = 0; i<10; i++)           // count nshort
    {
        nshort += results.rawbuf[3+2*i];
    }
    nshort /= 10;

    int i = 0;
    int j = 0;
    while(1)        // count nlong
    {
        if(results.rawbuf[4+2*i] > (2*nshort))
        {
            nlong += results.rawbuf[4+2*i];
            j++;
        }
        i++;
        if(j==10)break;
        if((4+2*i)>(count-10))break;
    }
    nlong /= j;

    int doubleshort = 2*nshort;
    for(i = 0; i<count_data; i++)
    {
        revData[i+D_DATA] = 0x00;
        for(j = 0; j<8; j++)
        {
            if(results.rawbuf[4 + 16*i + j*2] > doubleshort) // 1
            {
                revData[i+D_DATA] |= 0x01<< (7-j);
            }
            else
            {
                revData[i+D_DATA] &= ~(0x01<<(7-j));
            }
        }
    }
    revData[D_LEN]      = count_data+5;
    revData[D_STARTH]   = results.rawbuf[1];
    revData[D_STARTL]   = results.rawbuf[2];
    revData[D_SHORT]    = nshort;
    revData[D_LONG]     = nlong;
    revData[D_DATALEN]  = count_data;
 
//#if DEBUG_MODE
//    Serial.print("\r\n*************************************************************\r\n");
//    Serial.print("len\t = ");Serial.println(revData[D_LEN]);
//    Serial.print("start_h\t = ");Serial.println(revData[D_STARTH]);
//    Serial.print("start_l\t = ");Serial.println(revData[D_STARTL]);
//    Serial.print("short\t = ");Serial.println(revData[D_SHORT]);
//    Serial.print("long\t = ");Serial.println(revData[D_LONG]);
//    Serial.print("data_len = ");Serial.println(revData[D_DATALEN]);
//    for(int i = 0; i<revData[D_DATALEN]; i++)
//    {
//        Serial.print(revData[D_DATA+i]);Serial.print("\t");
//    }
//    Serial.print("\r\n*************************************************************\r\n");
//#endif

    irrecv.resume(); // Receive the next value
    return revData[D_LEN]+1;
}

//if get some data from IR
INT8U isIrData()
{
  
    if(irrecv.decode(&results))
    {
        int count       = results.rawlen;
        if(count < 64 || (count -4)%8 != 0)
        {
//#if DEBUG_MODE
//            Serial.print("IR GET BAD DATA!\r\n");
//#endif
            irrecv.resume();        // Receive the next value
            return 0;
        }
        int count_data  = (count-4) / 16;
//#if DEBUG_MDOE
//        Serial.print("ir get data! count_data = ");Serial.pirntln(count_data);
//#endif
        return (INT8U)(count_data+6);
    }
    else 
    {
        return 0;
    }
}
