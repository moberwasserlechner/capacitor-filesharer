require 'json'

  Pod::Spec.new do |s|
    # NPM package specification
    package = JSON.parse(File.read(File.join(File.dirname(__FILE__), 'package.json')))

    s.name = 'ByteowlsCapacitorFilesharer'
    s.version = package['version']
    s.summary = package['description']
    s.license = package['license']
    s.homepage = package['homepage']
    s.author = package['author']
    s.ios.deployment_target  = '12.0'
    s.dependency 'Capacitor'
    s.source = { :git => 'https://github.com/moberwasserlechner/capacitor-filesharer', :tag => s.version.to_s }
    s.source_files = 'ios/ByteowlsCapacitorFilesharer/Source/*.{swift,h,m}'
    s.swift_version = '5.0'
  end
