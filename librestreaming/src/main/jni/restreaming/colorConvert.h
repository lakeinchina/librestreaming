#ifndef __COLORCONVERT_H__
#define __COLORCONVERT_H__

void NV21TOYUV420SP(const unsigned char *src,const unsigned char *dst,int ySize);
void YUV420SPTOYUV420P(const unsigned char *src,const unsigned char *dst,int ySize);
void NV21TOYUV420P(const unsigned char *src,const unsigned char *dst,int ySize);
void NV21TOARGB(const unsigned char *src,const unsigned int *dst,int width,int height);
void NV21TOYUV(const unsigned char *src,const unsigned char *dstY,const unsigned char *dstU,const unsigned char *dstV,int width,int height);

#endif