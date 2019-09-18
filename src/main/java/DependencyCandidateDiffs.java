import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.jboss.shrinkwrap.resolver.api.maven.embedded.EmbeddedMaven;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Stream;

public class DependencyCandidateDiffs {


    public static void main(String[] args) throws IOException, GitAPIException {

        var repoDir = new File(String.format("/Users/jhejderup/Studies/uppdatera/%s/.git", args[0]));
        var builder = new FileRepositoryBuilder();
        try (var repository = builder.setGitDir(repoDir)
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build()) {
            System.out.println("Having repository: " + repository.getDirectory());
            var git = new Git(repository);

            //create a map with: new_commit -> old_commit
            var logs = git.log().all().call();
            var commitMap = new HashMap<RevCommit, RevCommit>();
            logs.forEach(rev -> {
                if (rev.getParents().length > 0)
                    commitMap.put(rev, rev.getParent(0));
                else
                    commitMap.put(rev, null);
            });

            var pomLogs = git.log().addPath("pom.xml").call();

            pomLogs.forEach(rev -> {
                try (var reader = repository.newObjectReader()) {
                    var oldTreeIter = new CanonicalTreeParser();
                    oldTreeIter.reset(reader, commitMap.get(rev).getTree());
                    var newTreeIter = new CanonicalTreeParser();
                    newTreeIter.reset(reader, rev.getTree());

                    var diffs = git.diff()
                            .setNewTree(newTreeIter)
                            .setOldTree(oldTreeIter)
                            .call();

                    var numSourceChanges = diffs.stream()
                            .filter(d -> !d.getNewPath().endsWith(".xml"))
                            .count();

                    var numModXMLfiles = diffs.stream()
                            .filter(d -> d.getChangeType() == DiffEntry.ChangeType.MODIFY)
                            .filter(d -> d.getNewPath().endsWith(".xml"))
                            .count();

                    //A diff worth checking for!
                    if (numSourceChanges == 0 && numModXMLfiles > 0) {
                        System.out.println(rev + " <- " + commitMap.get(rev));

                        git.checkout().setName(commitMap.get(rev).toObjectId().getName()).call();


                        var builtProject = EmbeddedMaven
                                .forProject(String.format("/Users/jhejderup/Studies/uppdatera/%s/pom.xml", args[0]))
                                .setQuiet()
                                .setGoals("help:effective-pom")
                                .build();

                        var oldDependencies = new HashMap<String, String>();

                        Stream.concat(
                                Stream.of(builtProject.getModel()),
                                builtProject.getModules().stream().map(m -> m.getModel())
                        ).flatMap(m -> Stream.concat(m.getDependencyManagement().getDependencies().stream(), m.getDependencies().stream()))

                                .forEach(m -> oldDependencies.put(m.getGroupId() + ":" + m.getArtifactId(), m.getVersion()));


                        git.checkout().setName(rev.toObjectId().getName()).call();

                        var build2 = EmbeddedMaven
                                .forProject(String.format("/Users/jhejderup/Studies/uppdatera/%s/pom.xml", args[0]))
                                .setQuiet()
                                .setGoals("help:effective-pom")
                                .build();

                        Stream.concat(
                                Stream.of(build2.getModel()),
                                build2.getModules().stream().map(m -> m.getModel())
                        ).flatMap(m -> Stream.concat(m.getDependencyManagement().getDependencies().stream(), m.getDependencies().stream()))
                                .filter(m -> !m.getGroupId().equals(build2.getModel().getGroupId()))
                                .forEach(m -> {
                                    if (oldDependencies.containsKey(m.getGroupId() + ":" + m.getArtifactId())) {
                                        var ver = oldDependencies.get(m.getGroupId() + ":" + m.getArtifactId());

                                        if (!Objects.equals(ver, m.getVersion())) {
                                            System.out.println(String.format("Dependency Change %s: %s -> %s", m.getGroupId() + ":" + m.getArtifactId(), ver, m.getVersion()));
                                        }

                                    }
                                });


                    }

                    git.checkout().setName("master").call();


                } catch (IncorrectObjectTypeException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (GitAPIException e) {
                    e.printStackTrace();
                }

            });


        }


    }
}
