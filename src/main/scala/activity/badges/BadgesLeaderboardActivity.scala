package org.fedoraproject.mobile

import Badges.JSONParsing._

import Implicits._

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast

import spray.json._

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher

import scala.concurrent.{ future, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.{ Failure, Success }

class BadgesLeaderboardActivity
  extends NavDrawerActivity
  with PullToRefreshAttacher.OnRefreshListener
  with util.Views {

  private lazy val refreshAdapter = new PullToRefreshAttacher(this)

  override def onPostCreate(bundle: Bundle) {
    super.onPostCreate(bundle)
    setUpNav(R.layout.badges_leaderboard_activity)
    updateLeaderboard(true)

    val lb = findView(TR.leaderboard)
    refreshAdapter.setRefreshableView(lb, this)
  }

  def onRefreshStarted(view: View): Unit = {
    updateLeaderboard(false)
    runOnUiThread(refreshAdapter.setRefreshComplete)
  }

  def updateLeaderboard(showProgress: Boolean): Unit = {
    if (showProgress) {
      findViewOpt(TR.progress).map(_.setVisibility(View.VISIBLE))
    }

    Badges.query("/leaderboard/json") onComplete {
      case Success(res) => {
        val lb = JsonParser(res).convertTo[Badges.Leaderboard]
        val adapter = new BadgesLeaderboardAdapter(
          this,
          android.R.layout.simple_list_item_1,
          lb.leaderboard.toArray)

        if (showProgress) {
          findViewOpt(TR.progress).map(v => runOnUiThread(v.setVisibility(View.GONE)))
        }
        findViewOpt(TR.leaderboard).map(v => runOnUiThread(v.setAdapter(adapter)))
      }
      case Failure(err) => {
        runOnUiThread(Toast.makeText(this, R.string.badges_lb_failure, Toast.LENGTH_LONG).show)
        Log.e("BadgesLeaderboardActivity", err.toString)
      }
    }
  }
}
