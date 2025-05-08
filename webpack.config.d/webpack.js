config.resolve.modules.push("../../processedResources/js/main");
config.resolve.conditionNames = ['import', 'require', 'default'];

if (config.devServer) {
    config.devServer.hot = true;
    config.devServer.compress = false; // workaround for SSE
    config.devtool = 'eval-cheap-source-map';
    config.devServer.headers = {'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Headers': '*', 'Access-Control-Allow-Methods': '*', 'allowedHosts': 'auto'}
} else {
    config.devtool = undefined;
}

// disable bundle size warning
config.performance = {
    assetFilter: function (assetFilename) {
      return !assetFilename.endsWith('.js');
    },
};
