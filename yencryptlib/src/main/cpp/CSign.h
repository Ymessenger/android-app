#pragma once
#include "CError.h"
namespace YSecurity
{
	class CSign : public CError
	{
	protected:
		size_t lenSign;
		unsigned char * sign;
	public:
		CSign(): lenSign(0), sign(nullptr)
		{
		}
		virtual ~CSign();

		size_t getLenSign();
		unsigned char * getSign();

		virtual void addMsg(unsigned char * msg, size_t lenMsg) = 0;
		virtual void subscribe() = 0;
		virtual void SetPrivetKey(unsigned char * key, size_t keyLen) = 0;
	};
}
