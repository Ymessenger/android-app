#pragma once
class CYDigest
{

public:
	CYDigest() {};
	virtual void addMsg(void * data, unsigned int lenData)=0;
	virtual void getDigest(void * byffer, unsigned int &lenBuffer)=0;
	virtual unsigned int getMaxSizeBuffer()=0;
	virtual ~CYDigest() {}
};