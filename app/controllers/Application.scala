package controllers

import play.api.mvc._
import org.kohsuke.github._
import collection.convert.wrapAll._
import lib._
import lib.Implicits._
import play.api.Logger
import concurrent.ExecutionContext.Implicits.global

object GitHubAccess {
  import play.api.Play.current

  val config = play.api.Play.configuration

  val githubApiKey = config.getString("github.apikey").get

  def conn() = GitHub.connectUsingOAuth(githubApiKey)

  val org = {
    val orgs = conn.getMyOrganizations.values
    require(orgs.size == 1, "The bot should have membership of exactly one org.")
    orgs.head
  }

  Logger.info(s"Bot org is ${org.getLogin}")

  lazy val auditDef = AuditDef(githubApiKey, org)
}

object Application extends Controller {

  def index = Action.async {
    val auditDef = GitHubAccess.auditDef

    for (orgSnapshot <- OrgSnapshot(auditDef)) yield {

      orgSnapshot.createIssuesForNewProblemUsers()

      orgSnapshot.updateExistingIssues()

      Ok
    }
  }

}
