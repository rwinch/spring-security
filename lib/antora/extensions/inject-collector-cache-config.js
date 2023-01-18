'use strict'

const BASE_COMMAND = 'gradlew -q -PbuildSrc.skipTests=true'
const JVM_ARGS='-Xmx3g -XX:+HeapDumpOnOutOfMemoryError'
const REPO_URL = 'https://github.com/spring-projects/spring-security'
const TASK_NAME=':spring-security-docs:generateAntora'

module.exports.register = function () {
  const expandPath = this.require('@antora/expand-path-helper')
  const fs = this.require('fs')
  const getUserCacheDir = this.require('cache-directory')
  this.once('contentAggregated', ({ playbook, contentAggregate }) => {
    for (const { origins } of contentAggregate) {
      for (const origin of origins) {
        const tag = origin.tag
        const original_collector = origin.descriptor.ext?.collector
        if (origin.url === REPO_URL && original_collector !== undefined && tag !== undefined) {
          const base_cache_dir = getUserCacheDir(`antora/collector-cache`)
          // FIXME use origin.gitdir + tag for unique name
          const cache_dir = `${base_cache_dir}/${tag}`
          if (!fs.existsSync(base_cache_dir)) {
              fs.mkdirSync(base_cache_dir, { recursive: true })
          }
          if (!fs.existsSync(cache_dir)) {
              // FIXME try and restore from URL by downloading zip
              console.log('Try and restore cache from downloading zip')
          }
          if (fs.existsSync(cache_dir)) {
              // use the cache
              origin.descriptor.ext.collector = {
                  scan: {
                      dir: cache_dir
                  }}
              console.log(`Use the cache ${tag}`)
          }
          else {
              // cache the output of the build
              console.log("Inject the cache")
              origin.descriptor.ext.collector = create_cached_collector(original_collector, cache_dir)
              // FIXME add the zip of cache to be published
          }

        }
      }
    }
  })
}

function create_cached_collector(original_collector, cache_dir) {
    const additional_collector = [
        {
            "run": {
                // FIXME get from scan location
                "command": `cp -r ./docs/build/generateAntora ${cache_dir}`
            }
        },
        {
            "run": {
                "command": `zip ${cache_dir}.zip -r ${cache_dir}`
            }
        }]
    if (Array.isArray(original_collector)) {
        // FIXME: Handle if it is array
        console.log('it is an array')
    }
    else {
      additional_collector.unshift(original_collector)
      return additional_collector
    }
}
