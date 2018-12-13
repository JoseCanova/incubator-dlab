import org.sonatype.nexus.repository.storage.WritePolicy
import org.sonatype.nexus.repository.maven.VersionPolicy
import org.sonatype.nexus.repository.maven.LayoutPolicy
import org.sonatype.nexus.common.entity.*
import org.sonatype.nexus.security.*
import org.sonatype.nexus.security.authz.*
import org.sonatype.nexus.ldap.persist.*
import org.sonatype.nexus.ldap.persist.entity.*
import org.sonatype.nexus.scheduling.TaskScheduler
import org.sonatype.nexus.scheduling.schedule.Daily
import org.sonatype.nexus.scheduling.schedule.Hourly

//Removing default repositories
repository.getRepositoryManager().delete('maven-central');
repository.getRepositoryManager().delete('maven-public');
repository.getRepositoryManager().delete('maven-releases');
repository.getRepositoryManager().delete('maven-snapshots');
repository.getRepositoryManager().delete('nuget-group');
repository.getRepositoryManager().delete('nuget-hosted');
repository.getRepositoryManager().delete('nuget.org-proxy');

//Creating custom repositories
blobStore.createFileBlobStore('artifacts_store', 'artifacts_store')
blobStore.createFileBlobStore('packages_store', 'packages_store')
blobStore.createFileBlobStore('docker_store', 'docker_store')
repository.createPyPiProxy('pypi','https://pypi.org/', 'packages_store', true)
repository.createMavenProxy('maven-central','https://repo1.maven.org/maven2/', 'artifacts_store', true, VersionPolicy.RELEASE, LayoutPolicy.PERMISSIVE)
repository.createDockerProxy('docker-hub', 'https://registry-1.docker.io', 'HUB', null, null, null, 'docker_store', true, true)
repository.createRawProxy('docker-rep','https://download.docker.com/linux/ubuntu', 'packages_store')
repository.createDockerGroup('docker-group', null, 8082, ['docker-hub'], true, 'docker_store')

log.info('Script rawRepositories completed successfully')