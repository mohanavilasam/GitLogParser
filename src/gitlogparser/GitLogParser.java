package gitlogparser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;



public class GitLogParser {
	private static final Logger LOGGER = Logger.getLogger( GitLogParser.class.getName() );
	private static MySqlConn mysqlconn;

	public static void main(String[] args) {

		System.out.println(GitLogParser.class.getClassLoader().getResource("logging.properties"));
		mysqlconn = new MySqlConn();
		mysqlconn = iniDb();
		Repository repository = null;
		FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
		repositoryBuilder.setMustExist(true);
		repositoryBuilder.setGitDir(new File("D:/compsac2017/ProjectsToReview/WordPress-Android/.git"));
		try {
			repository = repositoryBuilder.build();
		} catch (IOException e) {
			LOGGER.log( Level.SEVERE, e.toString(), e );
		}
		if (repository.getObjectDatabase().exists()) {
			Git git = new Git(repository);
			RevWalk walk = new RevWalk(repository);
			try {
				List<Ref> branches = git.branchList().call();

				for (Ref branch : branches) {
					String branchName = branch.getName();
					Iterable<RevCommit> commits = git.log().all().call();
					for (RevCommit commit : commits) {
						boolean foundInThisBranch = false;

						RevCommit targetCommit = walk.parseCommit(repository.resolve(commit.getName()));
						for (Map.Entry<String, Ref> e : repository.getAllRefs().entrySet()) {
							if (e.getKey().startsWith(Constants.R_HEADS)) {
								if (walk.isMergedInto(targetCommit, walk.parseCommit(e.getValue().getObjectId()))) {
									String foundInBranch = e.getValue().getName();
									if (branchName.equals(foundInBranch)) {
										foundInThisBranch = true;
										break;
									}
								}
							}
						}

						if (foundInThisBranch) {
							for (int i = 0; i < commit.getParentCount(); i++) {
								String issueInfoInsQuery = "INSERT INTO GitLogInfo(CommitSha, AuthorName, Message, ParentSHA, DiffInfo) VALUES ("
										+ " '" + commit.getName() 										+ "' " + " , " 
										+ " '" + commit.getAuthorIdent().getName().replaceAll("'", "")  + "' " + " , " 
										+ " '" + commit.getFullMessage().replaceAll("'", "") 			+ "' " + " , " 
										+ " '" + commit.getParent(i).getName() 							+ "' " + " , " 
										+ " '" + "XYZ" 													+ "' " 
										+ " );";
								mysqlconn.executeDmlStmt(issueInfoInsQuery);

							}

						}
					}

				}

			} catch (GitAPIException e) {
				LOGGER.log( Level.SEVERE, e.toString(), e );
			} catch (IOException e1) {
				LOGGER.log( Level.SEVERE, e1.toString(), e1 );
			}
		}

	}

	private static MySqlConn iniDb() {

		mysqlconn.executeDmlStmt("CREATE TABLE IF NOT EXISTS GitLogInfo " + 
		"(" + "ID int NOT NULL AUTO_INCREMENT, "
				+ "CommitSha VARCHAR(60), " + "AuthorName VARCHAR(60), " + "Message VARCHAR(530), "
				+ "ParentSHA VARCHAR(60), " + "DiffInfo VARCHAR(60)," + "primary key (ID)" + 
		");");
		return mysqlconn;
	}
}
