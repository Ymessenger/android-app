#pragma once

namespace YSecurity
{
	class CGenerator
	{
	public:
		CGenerator();
		virtual ~CGenerator();

		virtual void generate() = 0;
	};
}
