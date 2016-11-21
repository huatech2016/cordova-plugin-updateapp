#import "CDVUpdateApp.h"
#import <Cordova/CDVPluginResult.h>

@implementation CDVUpdateApp

NSString *ipaPath;


- (NSString *)getCurrentVersionCode {
    return [[[NSBundle mainBundle] infoDictionary] objectForKey:@"CFBundleVersion"];
}

- (NSString *)getCurrentVersionName {
    NSString* version = [[[NSBundle mainBundle] infoDictionary] objectForKey:@"CFBundleShortVersionString"];
    //    if (version == nil) {
    //        version = [[[NSBundle mainBundle] infoDictionary] objectForKey:@"CFBundleVersion"];
    //    }
    return version;     
}

- (NSDictionary *)getServerVersionCode:(NSString *)url {
    
    @try {
        NSError *error = nil;
        
        //加载一个NSURL对象
        NSURLRequest *request = [NSURLRequest requestWithURL:[NSURL URLWithString:url] cachePolicy:NSURLRequestReloadIgnoringCacheData timeoutInterval:60.0f];
        //将请求的url数据放到NSData对象中
        NSData *response = [NSURLConnection sendSynchronousRequest:request returningResponse:nil error:nil];
        //IOS5自带解析类NSJSONSerialization从response中解析出数据放到字典中
        NSDictionary *resultDic = [NSJSONSerialization JSONObjectWithData:response options:NSJSONReadingMutableLeaves error:&error];
        
        return resultDic;
    }
    @catch (NSException *exception) {
        return nil;
    }
}



-(void) hasNewVersion:(CDVInvokedUrlCommand *)command
{
    [self.commandDelegate runInBackground:^{
        
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK ];
        NSString *checkPath = [command argumentAtIndex:0];
        NSString *currentVersionName= [self getCurrentVersionName]; //获取 CFBundleShortVersionString 值
        
        @try {
            NSError *error = nil;
            NSDictionary *backResultDic;
            //加载一个NSURL对象
            NSURLRequest *request = [NSURLRequest requestWithURL:[NSURL URLWithString:checkPath] cachePolicy:NSURLRequestReloadIgnoringCacheData timeoutInterval:60.0f];
            //将请求的url数据放到NSData对象中
            NSData *response = [NSURLConnection sendSynchronousRequest:request returningResponse:nil error:nil];
            //IOS5自带解析类NSJSONSerialization从response中解析出数据放到字典中
            NSDictionary *resultDic = [NSJSONSerialization JSONObjectWithData:response options:NSJSONReadingMutableLeaves error:&error];
            if (resultDic == nil) {
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"error"];
            } else {
                
                NSString *lastVersion = [resultDic objectForKey:@"verName"];
                NSString *updateContent = [resultDic objectForKey:@"updateContent"];
                
                if ([lastVersion compare:currentVersionName options:NSNumericSearch] == NSOrderedDescending) {
                     backResultDic =[[NSDictionary alloc]initWithObjectsAndKeys:@YES,@"needUpdate",updateContent,@"updateContent",currentVersionName,@"currentVersion",lastVersion,@"newVersion",nil];
                }
                else
                {
                     backResultDic = [[NSDictionary alloc]initWithObjectsAndKeys:@NO,@"needUpdate",updateContent,@"updateContent",currentVersionName,@"currentVersion",lastVersion,@"newVersion",nil];
                    
                }
//                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString: [self dictionaryToJson:backResultDic]] ;
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:backResultDic] ;            }
            
            
        }
        @catch (NSException *exception) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"-1" ];
            
        }
        
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
    
}
- (void)downloadApp:(CDVInvokedUrlCommand *)command{
    [self.commandDelegate runInBackground:^{
        NSString *checkPath = [command argumentAtIndex:0];
        NSDictionary *resultDic = [self getServerVersionCode:checkPath];
        if (resultDic) {
            NSString *lastVersion = [resultDic objectForKey:@"verName"];
            ipaPath = [resultDic objectForKey:@"ipaPath"];
            NSString *itemService = @"itms-services://?action=download-manifest&url=";
            ipaPath =  [itemService stringByAppendingString:ipaPath];
            
            if ([lastVersion compare:[self getCurrentVersionCode] options:NSNumericSearch] == NSOrderedDescending) {
                NSURL *url = [NSURL URLWithString:ipaPath];
                [[UIApplication sharedApplication]openURL:url];
            }
        }
        
        
        
    }];
    
}
//- (NSString*)dictionaryToJson:(NSDictionary *)dic
//
//{
//    
//    NSError *parseError = nil;
//    
//    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:dic options:NSJSONWritingPrettyPrinted error:&parseError];
//    
//    return [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
//    
//}
//- (NSString *) getOffRubbishWithString:(NSString *)str
//{
//    NSMutableString *responseString = [NSMutableString stringWithString:str];
//    NSString *character = nil;
//    for (int i = 0; i < responseString.length; i ++) {
//        character = [responseString substringWithRange:NSMakeRange(i, 1)];
//        if ([character isEqualToString:@"\\"])
//            [responseString deleteCharactersInRange:NSMakeRange(i, 1)];
//    }
//    
//    [responseString stringByReplacingOccurrencesOfString:@"\'" withString:@""];
//    
//    return responseString;
//}
@end

