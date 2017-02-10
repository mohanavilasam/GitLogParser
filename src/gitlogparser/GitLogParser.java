package gitlogparser;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;



public class GitLogParser {
	private static final Logger LOGGER = Logger.getLogger( GitLogParser.class.getName() );
	private static MySqlConn mysqlconn;

	public static void main(String[] args) {

		//System.out.println(GitLogParser.class.getClassLoader().getResource("logging.properties"));
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
								ObjectId head = repository.resolve(commit.getName() + "^{tree}");
								ObjectId old = repository.resolve(commit.getParent(i).getName() + "^{tree}");
								ObjectReader reader = repository.newObjectReader();
								CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
								oldTreeIter.reset(reader, old);
								CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
								newTreeIter.reset(reader, head);
								List<DiffEntry> diffs = git.diff().setNewTree(newTreeIter).setOldTree(oldTreeIter).call();
								int diffCount = diffs.size();
								for (int k = 0; k < diffCount; k++) {
									//diffs.get(k).getChangeType().toString();
									//diffs.get(k).getOldPath();
									//diffs.get(k).getNewPath();
									String issueInfoInsQuery = "INSERT INTO GitLogInfo(CommitSha, AuthorName, Message, ParentSHA, DiffChangeType, DiffOldPath, DiffNewPath) VALUES ("
											+ " '" + commit.getName() 										+ "' " + " , " 
											+ " '" + commit.getAuthorIdent().getName().replaceAll("'", "")  + "' " + " , " 
											+ " '" + commit.getFullMessage().replaceAll("'", "") 			+ "' " + " , " 
											+ " '" + commit.getParent(i).getName() 							+ "' " + " , " 
											+ " '" + diffs.get(k).getChangeType().toString()				+ "' " + " , " 
											+ " '" + diffs.get(k).getOldPath()								+ "' " + " , " 
											+ " '" + diffs.get(k).getNewPath()								+ "' " 
											+ " );";
									mysqlconn.executeDmlStmt(issueInfoInsQuery);
									//System.out.println(issueInfoInsQuery);
								}								
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
				+ "ParentSHA VARCHAR(60), " + "DiffInfo VARCHAR(60)," 
				+ "DiffOldPath VARCHAR(255), " 
				+ "DiffNewPath VARCHAR(255), "
				+ "DiffChangeType VARCHAR(60), " 
				+ "primary key (ID)" + 
		");");
		return mysqlconn;
	}
}
