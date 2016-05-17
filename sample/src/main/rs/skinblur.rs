#pragma version(1)
#pragma rs java_package_name(me.lake.librestreaming.sample)
#pragma rs_fp_relaxed

rs_allocation gIn;

int width;
int height;
int size;
float radius;
int snumrad;
int maxdelta;

uchar __attribute__((kernel)) blur(uchar in,uint32_t x)
{
    if(x>size){
        return in;
    }
    int32_t yy=x/width;
    int32_t xx=x%width;
    uint32_t vpox = size + (yy/2 * width) + xx-xx%2;
    uint8_t vv = (*((uint8_t*)rsGetElementAt(gIn, vpox)));
    uint8_t uu = (*((uint8_t*)rsGetElementAt(gIn, ++vpox)));
    int32_t numrad;
    if ((uu > 70 && uu < 127) && (vv > 133 && vv < 180)) {
        numrad=snumrad;
    }else{
        return in;
    }

    uint8_t src_db = in;
    uint8_t src_b;
    uint32_t sum = 0;
    uint32_t fact = 0;
    uint32_t d;
    int j,i;
    uint32_t rowsum  = 0;
    uint32_t rowfact = 0;
    uint32_t offset;
    int tmp;

    for (j = 1 - numrad; j < numrad; j+=2)
    {
        rowsum  = 0;
        rowfact = 0;
        if (yy + j < 0 || yy + j >= height)
            continue;

        offset = (yy + j)*width+xx;
        for (i = 1 - numrad; i < numrad; i+=2)
        {
            if (xx + i < 0 || xx + i >= width)
                continue;

            src_b = (*((uint8_t*)rsGetElementAt(gIn, offset + i)));
            tmp = src_db - src_b;
            if (tmp > maxdelta || tmp < -maxdelta)
                continue;

            rowsum += src_b;
            ++rowfact;
        }
        sum += rowsum;
        fact += rowfact;
    }
    if (fact == 0){
        return  src_db;
    }
    else{
        return  sum / fact;
    }
}
