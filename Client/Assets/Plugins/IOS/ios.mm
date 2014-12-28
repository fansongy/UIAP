//
//  ios.mm
//  Unity-iPhone
//
//  Created by Fansy on 14/11/5.
//
//

#import "ios.h"
#import "IOSManager.h"

#ifdef __cplusplus
extern "C"
{
#endif
    void pay(const char* order);
    void getItems(const char* jsonList);
#ifdef __cplusplus
}
#endif

@implementation ios

void pay(const char* a)
{
    NSString* str = [[NSString alloc] initWithUTF8String:a];
    [[IOSManager getInstance] purchaseItem:str];
    
}

void getItems(const char* jsonList)
{
     NSString* str = [[NSString alloc] initWithUTF8String:jsonList];
    [[IOSManager getInstance] getItem:str];
}
@end