#pragma once
#include <string>

namespace YSecurity
{
	class CError
	{
	protected:
		int codeError;
		bool _isError;
		std::string lastError;

	public:
		CError();
		~CError();

		int getCodeLastError();
		bool isError();
		std::string getLastError();
	};

}