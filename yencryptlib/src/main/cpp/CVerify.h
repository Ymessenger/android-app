#pragma once
#include "CError.h"

namespace YSecurity
{
	class CVerify : public CError
	{
	protected:
		bool correct;
		bool isVerify;
		size_t lenSign;
		unsigned char * sign;
	public:
		CVerify();
		virtual ~CVerify();

		virtual void setPublicKey(unsigned char * sign, size_t sLen) = 0;
		virtual void addMsg(unsigned char * msg, size_t lenMsg) = 0;
		virtual void verify() = 0;

		void setSign(unsigned char * sign, size_t sLen);

		bool isCorrect();
	};
}

