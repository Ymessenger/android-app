/*
 * This file is part of Y messenger.
 *
 * Y messenger is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Y messenger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Y messenger.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.ymessenger.app.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import kotlinx.android.synthetic.main.activity_intro.*
import org.ymessenger.app.R
import org.ymessenger.app.adapters.intro.PageAdapter
import org.ymessenger.app.controllers.DotIndicatorController
import org.ymessenger.app.fragments.intro.IntroPageFragment
import org.ymessenger.app.models.IntroPageModel

class IntroActivity : AppCompatActivity() {

    private lateinit var pageAdapter: PageAdapter
    private lateinit var dotIndicatorController: DotIndicatorController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        pageAdapter = PageAdapter(createFragments(), supportFragmentManager)
        dotIndicatorController = DotIndicatorController(this, dotsContainerLinearLayout)
        dotIndicatorController.initialize(pageAdapter.count)
        viewPager.adapter = pageAdapter
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                if (position == pageAdapter.count - 1) {
                    btnNext.setText(R.string.close)
                } else {
                    btnNext.setText(R.string.next)
                }
                dotIndicatorController.setSelected(position)
            }
        })

        btnNext.setOnClickListener { clickNext() }
    }

    private fun createFragments(): ArrayList<Fragment> {
        val fragments: ArrayList<Fragment> = ArrayList()

        val welcomePage = IntroPageModel.Builder()
            .setTitle(getString(R.string.intro_welcome_title))
            .setDescription(getString(R.string.intro_welcome_description))
            .build()
        fragments.add(IntroPageFragment.newInstance(welcomePage))

        val privacyPage = IntroPageModel.Builder()
            .setImage(ContextCompat.getDrawable(this, R.drawable.ic_privacy_white))
            .setTitle(getString(R.string.intro_privacy_title))
            .setDescription(getString(R.string.intro_privacy_description))
            .build()
        fragments.add(IntroPageFragment.newInstance(privacyPage))

        val decentralizedPage = IntroPageModel.Builder()
            .setImage(ContextCompat.getDrawable(this, R.drawable.ic_decentralized))
            .setTitle(getString(R.string.intro_decentralized_title))
            .setDescription(getString(R.string.intro_decentralized_description))
            .build()
        fragments.add(IntroPageFragment.newInstance(decentralizedPage))

        val teamPage = IntroPageModel.Builder()
            .setImage(ContextCompat.getDrawable(this, R.drawable.ic_team))
            .setTitle(getString(R.string.intro_team_title))
            .setDescription(getString(R.string.intro_team_description))
            .build()
        fragments.add(IntroPageFragment.newInstance(teamPage))

        return fragments
    }

    private fun clickNext() {
        if (viewPager.currentItem == pageAdapter.count - 1)
            finish()

        if (viewPager.currentItem < pageAdapter.count - 1)
            viewPager.currentItem = viewPager.currentItem + 1
    }
}