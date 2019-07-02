import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class FetchGithubProjects {

   private static final String CODECOV  = "https://codecov.io/gh/";
   private static final String COVERALLS = "https://coveralls.io/";
    private static final String CODECLIMATE = "https://codeclimate.com/github/";



    public static boolean hasTravisCI(GHRepository repo) {
        try {
            repo.getFileContent(".travis.yml");
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean hasCodeCoverageService(GHRepository repo) {
        try {
            var content = repo.getFileContent("README.md");
            var reader = new BufferedReader(new InputStreamReader(content.read()));
            while(reader.ready()) {
                String line = reader.readLine();
                if(line.contains(CODECOV) || line.contains(COVERALLS) || line.contains(CODECLIMATE))
                    return true;
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }



    public static void main(String[] args) throws IOException {
        var github = GitHub.connect();

        System.out.println(github.getRateLimit());


//        var path = Paths.get("/Users/jhejderup/crap2.txt");


//        Files.readAllLines(path)
//                .stream()
//                .map(l -> l.split(","))
//                .map(ls ->String.format("https://github.com/%s/compare/%s..%s", ls[0],ls[1],ls[3]))
//                .forEach(System.out::println);


//        Files.readAllLines(path)
//                .stream()
//                .map(l -> l.split(","))
//                .map(ls -> {
//                    try {
//                        var nonbuildCount = Arrays.stream(github
//                                .getRepository(ls[0])
//                                .getCompare(ls[1],ls[3])
//                                .getFiles())
//                                .filter(k -> !k.getFileName().contains("pom.xml") && !k.getFileName().contains("build.gradle"))
//                                .count();
//
//                        if (nonbuildCount > 0){
//                            return null;
//                        } else {
//                            return ls;
//                        }
//                    } catch (IOException e) {
//                        return null;
//                    }
//                })
//                .filter(Objects::nonNull)
//                .map(ls -> String.join(",", ls))
//                .forEach(System.out::println);



//        var files = github.getRepository("gwtbootstrap3/gwtbootstrap3")
//                .getCompare("a04344eda3f16bcd70d822b03c2dfdf4e639848e","a96eb983b06ca5e271b5393b8710c46c6f105dc0")
//                .getFiles();




        var repos = github
                .searchRepositories()
                .language("java")
                .stars("100..500")
                .list();


        var repositories = StreamSupport
                .stream(repos.spliterator(), false);


        repositories
                .filter(repo -> Objects.equals(repo.getLanguage(), "Java"))
                .filter(FetchGithubProjects::hasTravisCI)
                .filter(FetchGithubProjects::hasCodeCoverageService)
                .forEach(repo -> System.out.println(String.format("%s.git", repo.getHtmlUrl().toString())));


//        repositories
//                .filter(repo -> Objects.equals(repo.getLanguage(), "Java"))
//                .filter(FetchGithubProjects::hasTravisCI)
//                .forEach(repo -> {
//
//
//                    try {
//                        var f = new File(String.format("/Users/jhejderup/Studies/uppdatera/%s", repo.getFullName()));
//                        Git.cloneRepository().setURI(String.format("%s.git", repo.getHtmlUrl().toString()))
//                                .setDirectory(f)
//                                .call();
//                        System.out.println(String.format("%s.git", repo.getHtmlUrl().toString()));
//                    } catch (GitAPIException e) {
//                        e.printStackTrace();
//                    }
//                });








    }
}
