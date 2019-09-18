import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.jboss.shrinkwrap.resolver.api.maven.embedded.EmbeddedMaven;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Stream;

public class MavenPOMDiff {


    public static void main(String[] args) throws IOException, GitAPIException {

        var repoDir = new File(String.format("/Users/jhejderup/Studies/uppdatera/%s/.git", args[0]));
        var builder = new FileRepositoryBuilder();
        try (var repository = builder.setGitDir(repoDir)
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build()) {
            System.out.println("Having repository: " + repository.getDirectory());

            var git = new Git(repository);


            git.checkout().setName(args[1]).call();


            var builtProject = EmbeddedMaven
                    .forProject(String.format("/Users/jhejderup/Studies/uppdatera/%s/pom.xml", args[0]))
                    .setQuiet()
                    .setGoals("dependency:tree")
                    .build();

            var oldDependencies = new HashMap<String, String>();

            Stream.concat(
                    Stream.of(builtProject.getModel()),
                    builtProject.getModules().stream().map(m -> m.getModel())
            ).flatMap(m -> Stream.concat(m.getDependencyManagement().getDependencies().stream(), m.getDependencies().stream()))

                    .forEach(m -> oldDependencies.put(m.getGroupId() + ":" + m.getArtifactId(), m.getVersion()));


            git.checkout().setName(args[2]).call();

            var build2 = EmbeddedMaven
                    .forProject(String.format("/Users/jhejderup/Studies/uppdatera/%s/pom.xml", args[0]))
                    .setQuiet()
                    .setGoals("dependency:tree")
                    .build();

            Stream.concat(
                    Stream.of(build2.getModel()),
                    build2.getModules().stream().map(m -> m.getModel())
            ).flatMap(m -> Stream.concat(m.getDependencyManagement().getDependencies().stream(), m.getDependencies().stream()))
                    .forEach(m -> {
                        if (oldDependencies.containsKey(m.getGroupId() + ":" + m.getArtifactId())) {
                            var ver = oldDependencies.get(m.getGroupId() + ":" + m.getArtifactId());

                            if (!Objects.equals(ver, m.getVersion())) {
                                System.out.println(String.format("Dependency Change %s: %s -> %s", m.getGroupId() + ":" + m.getArtifactId(), ver, m.getVersion()));
                            }

                        }
                    });


            git.checkout().setName("master").call();


        }


    }


}
